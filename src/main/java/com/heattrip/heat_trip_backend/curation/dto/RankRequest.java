package com.heattrip.heat_trip_backend.curation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 추천 요청 바디
 * - energy: -1/0/1
 * - socialNeed: [-1,1]
 * - goals: 예) ["nature_healing","quiet_reflection"]
 * - topN: 상위 장소 개수(기본 50)
 *
 * 🔥 추가 필드(필요 시만 전달)
 * - cat3Filter     : 특정 cat3 코드 제한 (LLM → cat3 바인딩 결과를 여기에)
 * - userLat/Lon  : 사용자 현재 위치 좌표 (mapy=lat, mapx=lon 가정)
 * - maxDistanceKm  : 반경 컷(km). null이면 제한 없음
 * - distanceWeight : 0.0~1.0 (0=거리무시, 1=거리강조). null이면 0.2
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankRequest {
    private PadDTO pad;
    private int energy;
    private double socialNeed;
    private List<String> goals;
    private Integer topK;

    // (옵션) LLM 전달용
    private String notes;
    private String moodKey;
    private String moodEmoji;

    private List<String> cat3Filter;
    private Double userLat;
    private Double userLng;
    private Double maxDistanceKm;
    private Double distanceWeight;

}
