package com.heattrip.heat_trip_backend.curation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * places 테이블 매핑
 * - PK: contentid (BIGINT)
 */
@Entity(name = "CurationPlace")
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @Column(name = "contentid", nullable = false)
    private Long id;

    @Column(name = "title")
    private String title;

    /** 세부 카테고리 코드(CAT3) */
    @Column(name = "cat3")
    private String cat3;

    /** 위경도(가정: mapy=lat, mapx=lon) */
    @Column(name = "mapx")
    private Double mapx;

    @Column(name = "mapy")
    private Double mapy;

    @Column(name = "firstimage")
    private String firstImage;
}
