package com.heattrip.heat_trip_backend.tour.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "places")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Place {
    @Id
    private Long contentid; // 불러온 XML의 contentid

    private String title;
    private String addr1;
    private String addr2;
    private String zipcode;

    private Double mapx;
    private Double mapy;

    private String firstimage;
    private String firstimage2;

    private String cat1;
    private String cat2;
    private String cat3;

    private Integer areacode;
    private Integer sigungucode;
    private Integer lDongRegnCd; // 법정동 시도 코드
    private Integer lDongSignguCd; // 법정동 시군구 코드
    private String tel;

    private String contenttypeid;

    private String createdtime;
    private String modifiedtime;
    private String mlevel;

    
}
