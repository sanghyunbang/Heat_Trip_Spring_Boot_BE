package com.heattrip.heat_trip_backend.curation.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * places 테이블 매핑
 * - PK: contentid (BIGINT)
 * - 이 엔티티는 추천에 필요한 최소 컬럼만 매핑(필요시 컬럼 추가 매핑)
 */
@Entity
@Table(name = "places")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Place {

    /** 장소 고유 ID (관광API contentid). 다른 테이블의 link_id와 1:1 매칭 */
    @Id
    @Column(name = "contentid", nullable = false)
    private Long id;

    /** 장소명 */
    @Column(name = "title")
    private String title;

    /** 세부 카테고리 코드(CAT3). 카테고리 추천 집계에 사용 */
    @Column(name = "cat3")
    private String cat3;

    /** 위경도(필요시 사용) */
    @Column(name = "mapx")
    private Double mapx;
    @Column(name = "mapy")
    private Double mapy;
}
