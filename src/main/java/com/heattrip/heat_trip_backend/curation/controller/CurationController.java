package com.heattrip.heat_trip_backend.curation.controller;

import com.heattrip.heat_trip_backend.curation.dto.CategoryScoreDTO;
import com.heattrip.heat_trip_backend.curation.dto.PlaceScoreDTO;
import com.heattrip.heat_trip_backend.curation.dto.RankRequest;
import com.heattrip.heat_trip_backend.curation.dto.RecommendResultDTO;
import com.heattrip.heat_trip_backend.curation.service.CurationRecommendService;
import com.heattrip.heat_trip_backend.curation.service.ScoringService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * curation 엔드포인트:
 * - /rank        : RankRequest(cat3Filter/거리 옵션 지원) 기반 랭킹
 * - /categories  : 감정 카테고리 집계
 * - /recommend   : LLM 라벨→CAT3→랭킹 (cat3Filter 없을 때만 LLM 호출)
 */
@Tag(name = "여행 추천 API", description = "사용자 감정(PAD) 기반의 장소 순위 및 카테고리 추천")
@RestController
@RequestMapping("/api/curation")
@RequiredArgsConstructor
public class CurationController {

    private final ScoringService scoring;
    private final CurationRecommendService orchestration;

    @PostMapping("/rank")
    public List<PlaceScoreDTO> rank(@RequestBody RankRequest req) {
        return scoring.rank(req);
    }

    @PostMapping("/categories")
    public List<CategoryScoreDTO> categories(@RequestBody RankRequest req) {
        return scoring.categories(req);
    }

    @PostMapping("/recommend")
    public RecommendResultDTO recommend(@RequestBody RankRequest req) {
        return orchestration.recommend(req);
    }
}
