package com.heattrip.heat_trip_backend.explore.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

// explore 디스플레이에 필요한 정보들
// 지역 기반으로 필터링 할 예정
// sigungu 시군구는 향후 쓸수도
public class PlaceSummaryDto {
    private Long contentid;
    private String title;
    private String firstimage;
    private Integer areacode;
    private Integer sigungucode;
    private LocalDateTime createdtime;
}
