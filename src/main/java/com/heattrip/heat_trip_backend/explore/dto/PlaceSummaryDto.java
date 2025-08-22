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
    
    // 플러터와 통일
    private String addr1;
    private String addr2;
    private String firstimage2;

    // 기존
    private String firstimage;
    private Integer areacode;
    private Integer sigungucode;
    private LocalDateTime createdtime;

    // 명시적 생성자 추가 (JPQL DTO 매핑용)
    public PlaceSummaryDto(Long contentid, String title, String firstimage,
                           Integer areacode, Integer sigungucode, LocalDateTime createdtime) {
        this.contentid = contentid;
        this.title = title;
        this.firstimage = firstimage;
        this.areacode = areacode;
        this.sigungucode = sigungucode;
        this.createdtime = createdtime;
    }


}
