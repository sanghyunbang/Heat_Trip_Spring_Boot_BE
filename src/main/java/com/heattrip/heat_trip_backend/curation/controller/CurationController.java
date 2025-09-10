package com.heattrip.heat_trip_backend.curation.controller;

import com.heattrip.heat_trip_backend.curation.dto.*;
import com.heattrip.heat_trip_backend.curation.service.ScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 추천 API 엔드포인트(MVP)
 * - POST /api/curation/rank : 장소 상위 N
 *   (카테고리 집계가 필요하면 이후 /categories 엔드포인트를 추가)
 */
@RestController
@RequestMapping("/api/curation")
@RequiredArgsConstructor
public class CurationController {

    private final ScoringService scoring;

    /** 장소 상위 N (기본 50). 바디는 RankRequest */
    @PostMapping("/rank")
    public List<PlaceScoreDTO> rank(@RequestBody RankRequest req) {
        return scoring.rank(req);
    }

    // 추가: 카테고리 집계
    @PostMapping("/categories")
    public List<CategoryScoreDTO> categories(@RequestBody RankRequest req) {
        return scoring.categories(req);
    }
}
