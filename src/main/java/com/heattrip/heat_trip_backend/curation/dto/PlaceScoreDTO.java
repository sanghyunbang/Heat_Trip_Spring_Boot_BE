package com.heattrip.heat_trip_backend.curation.dto;

import lombok.*;

/** 단일 장소 점수(설명용으로 trait/popularity/final 분리) */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlaceScoreDTO {
    private Long placeId;
    private String name;
    private String cat3Code;
    private double traitMatch;
    private double popularity;
    private double finalScore;
}
