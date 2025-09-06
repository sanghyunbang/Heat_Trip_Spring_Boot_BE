package com.heattrip.heat_trip_backend.explore.web;

import com.heattrip.heat_trip_backend.explore.dto.CursorPageResponse;
import com.heattrip.heat_trip_backend.explore.dto.PageResponse;
import com.heattrip.heat_trip_backend.explore.dto.PlaceDetailDto;
import com.heattrip.heat_trip_backend.explore.dto.PlaceSearchCond;
import com.heattrip.heat_trip_backend.explore.dto.PlaceSummaryDto;
import com.heattrip.heat_trip_backend.explore.service.ExploreService;

import org.slf4j.Logger;                      // ⬅ 추가
import org.slf4j.LoggerFactory; 

import org.springframework.validation.annotation.Validated;      // ← 메서드 파라미터 검증 활성화 (선택)
import jakarta.validation.constraints.Max;                  // ← size 등 파라미터 범위 검증 (선택)
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.*;

/**
 * Explore 도메인의 REST 컨트롤러.
 *
 * 엔드포인트 구성:
 * - GET /api/explore/places          : Offset(번호 기반) 페이지네이션 목록
 * - GET /api/explore/places/scroll   : Cursor(무한 스크롤) 페이지네이션 목록
 * - GET /api/explore/places/{id}     : 단건 상세 조회
 *
 * 컨트롤러는 "입력 파라미터 파싱/검증 → 서비스 호출 → DTO 반환"만 담당합니다.
 * 비즈니스 로직(페이징 전략, 매핑 등)은 ExploreService에서 수행합니다.
 *
 * MapStruct 사용 여부와는 무관하게 컨트롤러 코드는 동일합니다.
 * (MapStruct는 Service 내부에서 엔티티/프로젝션 → DTO 변환을 맡습니다.)
 */
@RestController
@RequestMapping("/api/explore/places")
@Validated // ← @Min/@Max 같은 메서드 파라미터 검증을 활성화 (선택)
public class ExploreController {

    private final ExploreService service;

    private static final Logger log = LoggerFactory.getLogger(ExploreController.class); // ⬅ 추가


    public ExploreController(ExploreService service) {
        this.service = service;
    }

    /**
     * 단건 상세 조회
     * 예) GET /api/explore/places/12345
     *
     * @param id 콘텐츠 PK(contentid)
     * @return PlaceDetailDto 상세 DTO
     */
    @GetMapping("/{id}")
    public PlaceDetailDto get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    /**
     * Offset(번호 페이지) 방식 목록 조회
     * - page: 0부터 시작 (기본값 0)
     * - size: 페이지당 개수 (기본값 20, 여기서는 검증으로 최대 100 제한)
     * - areacode/sigungucode/cat1/cat2/cat3: 필터 파라미터 (null이면 무시)
     *
     * 예)
     *   GET /api/explore/places?page=0&size=20&areacode=39&cat1=A01
     */
    @GetMapping
    public PageResponse<PlaceSummaryDto> list(
        @RequestParam(name = "areacode",  required = false) Integer areacode,
        @RequestParam(name = "sigungucode",  required = false) Integer sigungucode,
        @RequestParam(name = "cat1",  required = false) String cat1,
        @RequestParam(name = "cat2",  required = false) String cat2,
        @RequestParam(name = "cat3",  required = false) String cat3,
        @RequestParam(name = "page",  defaultValue = "0") @Min(0) int page,      // 음수 방지
        @RequestParam(name = "size",  defaultValue = "20") @Min(1) @Max(100) int size // 과도한 요청 방지
    ) {
        log.info("[GET] /api/explore/places?page={}&size={}&area={}&sigungu={}&cat1={}&cat2={}&cat3={}",
                 page, size, areacode, sigungucode, cat1, cat2, cat3);       // 디버깅 추가
        // 컨트롤러에서는 입력 파라미터를 간단히 DTO로 모아 서비스에 전달
        var cond = PlaceSearchCond.builder()
            .areacode(areacode)
            .sigungucode(sigungucode)
            .cat1(cat1)
            .cat2(cat2)
            .cat3(cat3)
            .build();

        // 실제 페이지네이션 로직(정렬, PageRequest 등)은 서비스가 담당
        return service.list(cond, page, size);
    }

    /**
     * Cursor(무한 스크롤) 방식 목록 조회
     * - cursor: 이전 응답에서 받은 nextCursor를 그대로 전달 (없으면 첫 페이지)
     * - size  : 요청 개수 (기본값 20, 최대 100)
     * - 필터가 바뀌면 cursor를 보내지 말고 첫 페이지부터 다시 시작해야 합니다.
     *
     * 예)
     *   1) 첫 페이지:
     *      GET /api/explore/places/scroll?areacode=39&size=20
     *   2) 다음 페이지:
     *      GET /api/explore/places/scroll?areacode=39&size=20&cursor=eyJ... (Base64URL)
     */
   @GetMapping("/scroll")
    public CursorPageResponse<PlaceSummaryDto> scroll(
        @RequestParam(name = "areacode",    required = false) Integer areacode,
        @RequestParam(name = "sigungucode", required = false) Integer sigungucode,
        @RequestParam(name = "cat1",        required = false) String  cat1,
        @RequestParam(name = "cat2",        required = false) String  cat2,
        @RequestParam(name = "cat3",        required = false) String  cat3Csv, 
        @RequestParam(name = "cursor",      required = false) String  cursor,
        @RequestParam(name = "size",        defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info("[GET] /api/explore/places/scroll?area={}&sigungu={}&cat1={}&cat2={}&cat3={}&cursor='{}'&size={}",
                 areacode, sigungucode, cat1, cat2, cat3Csv, cursor, size);     // ⬅ 추가

        var cond = PlaceSearchCond.builder()
            .areacode(areacode).sigungucode(sigungucode)
            .cat1(cat1).cat2(cat2).cat3(cat3Csv) // Csv를 그대로 cond에 넣음 -> Service에서 리스트로 바꿔서 담을 예정
            .build();

        try {
            return service.scroll(cond, cursor, size);  // ⬅ 에러 발생 지점
        } catch (Exception e) {
            log.error("scroll() failed: area={} sigungu={} cat1={} cat2={} cat3={} cursor='{}' size={}",
                      areacode, sigungucode, cat1, cat2, cat3Csv, cursor, size, e); // ⬅ 스택 출력
            throw e; // 전역 핸들러로 전달
        }
    }
}
