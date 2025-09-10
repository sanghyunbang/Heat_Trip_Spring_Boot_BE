package com.heattrip.heat_trip_backend.curation.entity;

import jakarta.persistence.*;
import lombok.*;

/** 18개 감정 카테고리 메타(이름/이모지/성격 요약) */
@Entity
@Table(name = "emotion_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmotionCategory {
    @Id @Column(name = "category_id")  private Integer id;
    @Column(name = "category_name")    private String name;
    @Column(name = "category_emoji")   private String emoji;
    @Column(name = "characteristic")   private String characteristic;
}
