package com.heattrip.heat_trip_backend.explore.service;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heattrip.heat_trip_backend.explore.dto.CursorPageResponse;
import com.heattrip.heat_trip_backend.explore.dto.PageResponse;
import com.heattrip.heat_trip_backend.explore.dto.PlaceDetailDto;
import com.heattrip.heat_trip_backend.explore.dto.PlaceSearchCond;
import com.heattrip.heat_trip_backend.explore.dto.PlaceSummaryDto;
import com.heattrip.heat_trip_backend.explore.mapper.PlaceMapper;               // ← MapStruct 매퍼 주입 (Entity/Projection → DTO 변환 담당)
import com.heattrip.heat_trip_backend.explore.repository.PlaceQueryRepository; // ← 네이티브 + 커서(Keyset) 페이지네이션용 Repository
import com.heattrip.heat_trip_backend.explore.repository.ExplorePlaceRepository;      // ← JPQL + 오프셋(페이지/사이즈) 페이지네이션용 Repository
import com.heattrip.heat_trip_backend.explore.repository.PlaceSummaryProjection;
import com.heattrip.heat_trip_backend.tour.domain.Place;

/**
 * Explore 도메인의 조회용 비즈니스 로직 계층.
 *
 * 책임(Responsibilities)
 * - 트랜잭션 경계 설정(@Transactional(readOnly = true)).
 * - 입력 검증(간단한 size 상한 등), 페이지네이션 전략(Offset / Cursor) 선택 및 실행.
 * - 엔티티/프로젝션을 API 응답 DTO로 매핑(MapStruct).
 *
 * 설계 요점(Why)
 * - Repository는 "데이터 접근"까지 담당, 이 Service는 "규칙/흐름/조합"을 담당.
 * - 오프셋 페이지네이션(list)과 커서 페이지네이션(scroll)을 분리 제공:
 *   · 오프셋: 총 개수 집계/임의 페이지 접근이 쉬움(대신 대용량에서는 느릴 수 있음).
 *   · 커서  : 무한 스크롤/다음 페이지만 빠르게(대용량에서 안정적, 중간 삽입/삭제에 강함).
 */
@Service
@Transactional(readOnly = true) // ← 읽기 전용 트랜잭션: JPA 변경 감지 비활성 최적화(일부 DB 힌트/캐시 유리)
public class ExploreService {

    private final ExplorePlaceRepository repo;            // ← Offset 페이지네이션용(JPQL로 DTO 생성 반환)
    private final PlaceQueryRepository cursorRepo; // ← Cursor 페이지네이션용(네이티브 + Projection)
    private final PlaceMapper mapper;              // ← MapStruct 매퍼(컴파일타임 생성 코드로 고성능 매핑)

    public ExploreService(
        ExplorePlaceRepository repo,
        PlaceQueryRepository cursorRepo,
        PlaceMapper mapper
    ) {
        this.repo = repo;
        this.cursorRepo = cursorRepo;
        this.mapper = mapper;
    }

    /** 
     * 상세 조회
     * - 책임: 단건 엔티티 로딩 → 상세 DTO로 매핑.
     * - 예외: 존재하지 않는 경우 NoSuchElementException 발생(컨트롤러에서 404로 매핑 권장).
     */
    public PlaceDetailDto get(Long id) {
        Place p = repo.findById(id).orElseThrow(() -> new NoSuchElementException("Place not found: " + id));
        // 엔티티 → DTO 변환(필드명/타입 변환, 필요시 추가 가공은 MapStruct 매퍼에서 정의)
        return mapper.toDetail(p);
    }

    /**
     * Offset 페이지네이션(전통적 page/size 방식).
     *
     * 동작
     * - page, size 기반으로 PageRequest 생성.
     * - 정렬은 createdtime DESC, contentid DESC (최신순 + 보조 키로 안정화).
     * - repo.findSummaries(...) 가 JPQL에서 DTO를 직접 생성하므로 추가 매핑 불필요.
     *
     * 장단점
     * - 장점: 임의 페이지 점프, 총 개수(totalElements) 제공 쉬움.
     * - 단점: page가 커질수록 OFFSET 스캔 비용 증가(대용량 비추천).
     */
    public PageResponse<PlaceSummaryDto> list(PlaceSearchCond c, int page, int size) {
        int safeSize = Math.max(1, Math.min(size, 100)); // ← 안전장치: 최소 1, 최대 100(과도한 page size 방지)
        var sort = Sort.by(Sort.Order.desc("createdtime"), Sort.Order.desc("contentid")); // ← 안정 정렬(동시간대 contentid로 확정)
        var pageable = PageRequest.of(Math.max(page, 0), safeSize, sort);                  // ← page 하한 0 보정

        // JPQL에서 new PlaceSummaryDto(...) 로 직접 생성해서 반환 → 여기서 별도 매핑 없음
        Page<PlaceSummaryDto> p = repo.findSummaries(
            c.getAreacode(), c.getSigungucode(),
            c.getCat1(), c.getCat2(), c.getCat3(),
            pageable
        );

        // Page → API 응답 래퍼로 재구성(페이지 메타 정보 포함)
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.isLast());
    }

    /**
     * Cursor(무한 스크롤) 페이지네이션.
     *
     * 핵심 개념(Keyset Pagination)
     * - 정렬 키 쌍(createdtime DESC, contentid DESC)을 "커서"로 인코딩하여,
     *   "이 커서보다 뒤(=더 과거)"의 레코드를 다음 페이지로 가져옴.
     * - OFFSET 미사용 → 대용량에서도 성능/안정성 우수. 중간 삽입/삭제에도 누락/중복 최소화.
     *
     * 커서 포맷
     * - cursor = Base64URL("epochMillis:contentid")
     *   · epochMillis: createdtime을 UTC 기준 epoch milli로 변환했다는 가정(아래 nextCursor 생성 로직 참고).
     *   · contentid  : 동일 timestamp에서의 안정 보조 키.
     *
     * 주의
     * - 커서가 변조/구버전일 수 있으므로 실제 운영에서는 try-catch로 파싱 실패 시 첫 페이지 처리 권장(여긴 "주석만" 요구라 코드 변경 없음).
     * - DB의 createdtime 타임존/타입과 앱의 변환 기준(UTC/KST)이 다르면 커서 불일치가 생길 수 있음(아래 ⑥ 설명 참고).
     */
    public CursorPageResponse<PlaceSummaryDto> scroll(PlaceSearchCond c, String cursor, int size) {
        int safeSize = Math.max(1, Math.min(size, 100)); // ← 안전장치
        int sizePlusOne = safeSize + 1;                  // ← +1 오버페치로 hasNext 판정

        // ① 커서 디코딩 (없으면 첫 페이지)
        Timestamp afterCreated = null; // ← 이전 페이지 마지막 createdtime (이하의 쿼리 WHERE에서 '작다' 조건에 쓰임)
        Long afterId = null;           // ← 보조 키(contentid)
        if (cursor != null && !cursor.isBlank()) {
            // cursor 포맷: Base64URL("millis:id")
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8); // ← URL-safe Base64 디코드
            String[] parts = raw.split(":");
            long millis = Long.parseLong(parts[0]); // ← epoch milli → Timestamp 복원
            afterCreated = new Timestamp(millis);
            afterId = Long.parseLong(parts[1]);
            // [주의] 예외 처리 없음: 숫자 아님/포맷 깨짐 등은 런타임 예외 가능 → 운영에서는 try/catch로 안전 처리 권장
        }

        // ② Pageable(pageSize=size+1, offset=0)
        // - 네이티브 쿼리에서 LIMIT 바인딩 이슈를 피하기 위해 Pageable을 사용.
        // - 커서 페이징에서는 OFFSET=0 고정 사용(OFFSET과 커서를 혼합하면 의미가 꼬일 수 있음).
        var pageable = PageRequest.of(0, sizePlusOne);

        // ③ 네이티브 쿼리 실행 → Projection 목록 수신(필요 필드만 얇게 가져옴)
        List<PlaceSummaryProjection> rows = cursorRepo.findNextByCursor(
            c.getAreacode(), c.getSigungucode(), c.getCat1(), c.getCat2(), c.getCat3(),
            afterCreated, afterId,
            pageable
        );

        // ④ 다음 페이지 존재 여부(+1 오버페치로 판정)
        boolean hasNext = rows.size() > safeSize;

        // ⑤ 반환 크기만큼 자르기 & Projection → DTO 변환(MapStruct)
        var slice = rows.stream().limit(safeSize).toList();
        List<PlaceSummaryDto> items = slice.stream()
            .map(mapper::toSummary)    // ← MapStruct가 타입 변환(Timestamp → LocalDateTime 등) 포함 처리
            .toList();
        // [주의] 매핑 성능: 수천 건/초 단위 트래픽이면 MapStruct는 충분히 빠름. 더 최적화가 필요하면 Projection 대신 DTO 직접 select 고려(네이티브에서도 가능).

        // ⑥ nextCursor 구성: 마지막 항목의 (createdtime, contentid)
        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            var last = items.get(items.size() - 1);
            long millis = last.getCreatedtime()
                // DB timestamp를 "UTC로 해석"하여 epoch milli로 변환한다는 가정.
                // - 만약 DB가 KST로 저장/해석된다면 ZoneOffset.UTC 대신 ZoneOffset.ofHours(9)를 사용해야 커서 일관성 유지.
                // - 여기서는 "코드 변경 없이 주석만" 요구이므로 설명만 남김.
                .atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
            nextCursor = Base64.getUrlEncoder()
                .encodeToString((millis + ":" + last.getContentid()).getBytes(StandardCharsets.UTF_8));
            // [포맷] Base64 URL-safe 인코딩 → 쿼리스트링/경로에 안전하게 포함 가능('+' '/' '=' 문제 회피)
        }

        // ⑦ 결과 포장: 아이템, 다음 커서, 다음 존재 여부
        // - 클라이언트는 nextCursor 가 null 이 아니면 이어서 호출.
        // - 데이터가 추가/삭제되어도 커서 기준 정렬이 안정성을 보장(동시간대 contentid로 타이브레이크).
        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }
}
