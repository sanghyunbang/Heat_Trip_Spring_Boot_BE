package com.heattrip.heat_trip_backend.explore.web;

import com.heattrip.heat_trip_backend.explore.dto.CursorPageResponse;
import com.heattrip.heat_trip_backend.explore.dto.PageResponse;
import com.heattrip.heat_trip_backend.explore.dto.PlaceDetailDto;
import com.heattrip.heat_trip_backend.explore.dto.PlaceSearchCond;
import com.heattrip.heat_trip_backend.explore.dto.PlaceSummaryDto;

// ▼ 감정/특성 DTO (새로 추가된 API용)
import com.heattrip.heat_trip_backend.explore.dto.PlaceFeaturesDto;
import com.heattrip.heat_trip_backend.explore.dto.EmotionalReviewDto;
import com.heattrip.heat_trip_backend.explore.dto.FeedbackRequestDto;
import com.heattrip.heat_trip_backend.explore.dto.FeedbackResponseDto;

import com.heattrip.heat_trip_backend.explore.service.ExploreEmotionService;
import com.heattrip.heat_trip_backend.explore.service.ExploreService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Stream;

/**
 * Explore 도메인의 REST 컨트롤러.
 *
 * Base Path: /api/explore/places
 *
 * 엔드포인트 구성:
 * - GET /api/explore/places                : Offset(번호 기반) 페이지네이션 목록
 * - GET /api/explore/places/scroll         : Cursor(무한 스크롤) 페이지네이션 목록
 * - GET /api/explore/places/{id}           : 단건 상세 조회 (숫자 ID만 허용)
 * - GET /api/explore/places/{contentId}/features
 * - GET /api/explore/places/{contentId}/emotional-reviews
 * - POST /api/explore/places/{contentId}/feedback
 *
 * 컨트롤러는 "입력 파라미터 파싱/검증 → 서비스 호출 → DTO/ResponseEntity 반환"에 집중합니다.①
 */
@RestController
@RequestMapping("/api/explore/places")
@Validated // @Min/@Max 등의 메서드 파라미터 검증 활성화
public class ExploreController {

    private static final Logger log = LoggerFactory.getLogger(ExploreController.class);

    private final ExploreService service;
    private final ExploreEmotionService emotionService;

    // ❗ 기존 코드의 버그 수정: emotionService를 주입받지 않고 this.emotionService = emotionService; 하던 부분을 수정
    public ExploreController(ExploreService service, ExploreEmotionService emotionService) {
        this.service = service;
        this.emotionService = emotionService;
    }

    // ─────────────────────────────────────────────────────────────────────
    // 1) 단건 상세 조회
    //   - scroll 과 경로 충돌 방지를 위해 숫자만 허용하는 정규식 적용(/{id:\d+}) ②
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/{id:\\d+}")
    public PlaceDetailDto get(@PathVariable("id") Long id) {
        return service.get(id);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 2) Offset(번호 페이지) 방식 목록 조회
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping
    public PageResponse<PlaceSummaryDto> list(
        @RequestParam(name = "areacode",    required = false) Integer areacode,
        @RequestParam(name = "sigungucode", required = false) Integer sigungucode,
        @RequestParam(name = "cat1",        required = false) String  cat1,
        @RequestParam(name = "cat2",        required = false) String  cat2,
        @RequestParam(name = "cat3",        required = false) String  cat3,
        @RequestParam(name = "page",        defaultValue = "0")  @Min(0)        int page,
        @RequestParam(name = "size",        defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info("[GET] /api/explore/places?page={}&size={}&area={}&sigungu={}&cat1={}&cat2={}&cat3={}",
                 page, size, areacode, sigungucode, cat1, cat2, cat3);

        var cond = PlaceSearchCond.builder()
            .areacode(areacode)
            .sigungucode(sigungucode)
            .cat1(cat1)
            .cat2(cat2)
            .cat3(cat3)
            .build();

        return service.list(cond, page, size);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 3) Cursor(무한 스크롤) 방식 목록 조회
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping("/scroll")
    public CursorPageResponse<PlaceSummaryDto> scroll(
        @RequestParam(name = "areacode",    required = false) Integer areacode,
        @RequestParam(name = "sigungucode", required = false) Integer sigungucode,
        @RequestParam(name = "cat1",        required = false) String  cat1,
        @RequestParam(name = "cat2",        required = false) String  cat2,
        @RequestParam(name = "cat3",        required = false) String  cat3Csv, // 서비스에서 CSV → 리스트로 파싱
        @RequestParam(name = "cursor",      required = false) String  cursor,
        @RequestParam(name = "size",        defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info("[GET] /api/explore/places/scroll?area={}&sigungu={}&cat1={}&cat2={}&cat3={}&cursor='{}'&size={}",
                 areacode, sigungucode, cat1, cat2, cat3Csv, cursor, size);

        var cond = PlaceSearchCond.builder()
            .areacode(areacode)
            .sigungucode(sigungucode)
            .cat1(cat1)
            .cat2(cat2)
            .cat3(cat3Csv) // 서비스에서 CSV 파싱 예정
            .build();

        try {
            return service.scroll(cond, cursor, size);
        } catch (Exception e) {
            log.error("scroll() failed: area={} sigungu={} cat1={} cat2={} cat3={} cursor='{}' size={}",
                      areacode, sigungucode, cat1, cat2, cat3Csv, cursor, size, e);
            throw e; // 전역 예외 핸들러에서 처리(로깅/응답 변환)
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // 4) 감정/공간특성 API (신규)
    // Base: /api/explore/places/{contentId}/...
    // ─────────────────────────────────────────────────────────────────────

    /**
     * 공간 특성 조회
     * 예) GET /api/explore/places/3386455/features?includeConf=true
     *
     * includeConf=false 로 보내면 conf_* / n_reviews 등 메타 필드를 제거하여
     * 응답 payload를 가볍게 할 수 있습니다.③
     */
    @GetMapping("/{contentId:\\d+}/features")
    public ResponseEntity<PlaceFeaturesDto> getPlaceFeatures(
        @PathVariable(name = "contentId") @Min(1) Long contentId,   // ← 이름 명시
        @RequestParam(name = "includeConf", defaultValue = "true") boolean includeConf
    ) {
        PlaceFeaturesDto dto = emotionService.getFeatures(contentId);
        if (dto == null) return ResponseEntity.notFound().build();

        if (!includeConf) {
            dto.setN_reviews(null);
            dto.setN_blogs(null);
            dto.setConf_sociality(null);
            dto.setConf_spirituality(null);
            dto.setConf_adventure(null);
            dto.setConf_culture(null);
            dto.setConf_nature_healing(null);
            dto.setConf_quiet(null);
            dto.setMethod_ver(null);
            dto.setUpdated_at(null);
        }
        return ResponseEntity.ok(dto);
    }

    /**
     * 감정 리뷰 목록
     * 예) GET /api/explore/places/3386455/emotional-reviews
     *
     * 필요 시 page/size 파라미터를 추가하여 페이징할 수 있습니다.④
     */
    @GetMapping("/{contentId:\\d+}/emotional-reviews")
    public ResponseEntity<List<EmotionalReviewDto>> getEmotionalReviews(
        @PathVariable(name = "contentId") @Min(1) Long contentId     // ← 이름 명시
    ) {
        List<EmotionalReviewDto> list = emotionService.getReviews(contentId);
        return ResponseEntity.ok(list);
    }

    /**
     * 나의 경험(피드백) 제출
     * 예) POST /api/explore/places/3386455/feedback
     * Body:
     * {
     *   "beforeEmotion":"SAD",
     *   "afterEmotion":"PROUD",
     *   "featureRatings":{"sociality":0.4,"spirituality":0.8,...},
     *   "content":"혼자였지만 힐링됐어요",
     *   "timestamp":"2025-09-08T14:22:00"
     * }
     */
    @PostMapping("/{contentId:\\d+}/feedback")
    public ResponseEntity<FeedbackResponseDto> postFeedback(
        @PathVariable(name = "contentId") @Min(1) Long contentId,    // ← 이름 명시
        @Valid @RequestBody FeedbackRequestDto request
    ) {
        FeedbackResponseDto res = emotionService.submitFeedback(contentId, request);
        return ResponseEntity.status(201).body(res);
    }

    // ★ 신규 포트/어댑터 검색
    @GetMapping("/search")
    public PageResponse<PlaceSummaryDto> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "contentTypeId", required = false) Integer contentTypeId,
            @RequestParam(name = "cat3", required = false) String cat3,           // CSV
            @RequestParam(name = "emotionCategoryId", required = false) Integer emotionCategoryId,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "20") Integer size,
            @RequestParam(name = "sort", required = false) String sort
    ) {
  List<String> cat3list = (cat3 == null || cat3.isBlank())
        ? List.of()
        : Stream.of(cat3.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList(); // Java 16 미만이면 .collect(Collectors.toList())

        var cond = new com.heattrip.heat_trip_backend.explore.dto.search.PlaceSearchCond(
                q, contentTypeId, cat3list, emotionCategoryId, page, size, sort
        );
        return service.searchAdvanced(cond);
    }
}
