package com.heattrip.heat_trip_backend.curation.dto;

import lombok.*;
import java.util.List;

/** 카테고리별 집계 결과(대표 장소 포함) */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryScoreDTO {
    private Integer categoryId;
    private String categoryName;
    private String emoji;
    private double score;
    private List<PlaceScoreDTO> topPlaces;
}
