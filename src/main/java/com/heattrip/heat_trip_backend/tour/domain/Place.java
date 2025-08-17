package com.heattrip.heat_trip_backend.tour.domain;

import java.time.LocalDateTime;

import com.heattrip.heat_trip_backend.tour.util.Sanitizers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
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

    // 예시: 전화번호는 100자로 제한(스키마와 맞추기)
    public static final int TEL_MAX_LEN = 100;
    public static final int TITLE_MAX_LEN = 255;
    public static final int ADDR_MAX_LEN = 255;
    public static final int ZIPCODE_MAX_LEN = 20;
    
    @Id
    private Long contentid; // 불러온 XML의 contentid

    @Size(max = TITLE_MAX_LEN)
    @Column(length = TITLE_MAX_LEN)
    private String title;
    
    @Size(max = ADDR_MAX_LEN)
    @Column(length = ADDR_MAX_LEN)
    private String addr1;
    
    @Size(max = ADDR_MAX_LEN)
    @Column(length = ADDR_MAX_LEN)
    private String addr2;

    @Size(max = ZIPCODE_MAX_LEN)
    @Column(length = ZIPCODE_MAX_LEN)    
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

    @Size(max = TEL_MAX_LEN)
    @Column(length = TEL_MAX_LEN)    
    private String tel;

    private String contenttypeid;

    // private String createdtime;
    // private String modifiedtime;
    // 빠른 정렬/필터링 위해서 varchar -> LocalDateTime으로 타입 변경
    // DB도 직접 쿼리문으로 타입 변경하기
    // DTO도 변경
    private LocalDateTime createdtime;
    private LocalDateTime modifiedtime;
    
    private String mlevel;

    @PrePersist
    @PreUpdate
    private void sanitizeAndTruncate() {
        this.tel = Sanitizers.cleanTel(this.tel, TEL_MAX_LEN);
        this.title = Sanitizers.truncate(this.title, TITLE_MAX_LEN);
        this.addr1 = Sanitizers.truncate(this.addr1, ADDR_MAX_LEN);
        this.addr2 = Sanitizers.truncate(this.addr2, ADDR_MAX_LEN);
        this.zipcode = Sanitizers.truncate(this.zipcode, ZIPCODE_MAX_LEN);
    }
}