package com.heattrip.heat_trip_backend.tour.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * [place_trait_snapshots] : "물리적 캐시 테이블"의 엔티티.
 * - 매일 places(개별 장소) 수집 후, 애플리케이션이 place_traits(고정 특성)와
 *   해시태그/심플태그/설명을 일괄 조인한 결과를 저장합니다.
 * - API는 이 테이블만 조회하면 되므로, 런타임 조인을 피할 수 있어 응답이 빠르고 단순합니다.
 * - FK는 걸지 않습니다(값 복제 캐시). 실패 시 롤백되도록 트랜잭션으로 감쌉니다.
 */
@Entity
@Table(name = "place_trait_snapshots",
       indexes = { @Index(name="idx_pts_cat3", columnList="cat3") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlaceTraitSnapshot {

    /** PK: 개별 장소의 고유 ID (places.contentid와 동일) */
    @Id
    private Long contentId;

    // ---- 원천: places ----
    /** places.cat3 (FK 없음; 값만 보관) */
    @Column(length = 16)
    private String cat3;

    // ---- 원천: place_traits ----
    /** 분류명(예: "고택") */
    @Column(length = 200)
    private String cat3Name;

    /** 성향 점수들 (traits에서 복제) */
    private Double pScore;
    private Double aScore;
    private Double dScore;
    private Double sociality;
    private Double noise;
    private Double crowdness;

    // ---- 파생: 해시태그/심플태그/설명 (JSON 및 LOB로 저장) ----
    @Lob @Convert(converter = StringListJsonConverter.class)
    private List<String> hashtags;

    @Lob @Convert(converter = StringListJsonConverter.class)
    private List<String> simpleTags;

    @Lob private String shortDesc1;
    @Lob private String shortDesc2;

    /** 스냅샷 생성 시각(모니터링/디버깅용) */
    private LocalDateTime snapshotAt;
}
