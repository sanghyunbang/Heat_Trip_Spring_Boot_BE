package com.heattrip.heat_trip_backend.curation.dto;

import lombok.*;
import java.util.List;

/**
 * 추천 요청 바디
 * - energy: -1/0/1
 * - socialNeed: [-1,1]
 * - goals: 예) ["nature_healing","quiet_reflection"]
 * - topN: 상위 장소 개수(기본 50)
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RankRequest {
    private PadDTO pad;
    private int energy;
    private double socialNeed;
    private List<String> goals;
    private Integer topN;
}
