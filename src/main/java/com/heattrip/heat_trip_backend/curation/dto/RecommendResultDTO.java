// com.heattrip.heat_trip_backend.curation.dto.RecommendResultDTO
package com.heattrip.heat_trip_backend.curation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecommendResultDTO {

    /** 최종 랭킹 (지금까지 반환하던 그 리스트) */
    private List<PlaceScoreDTO> places;

    /** LLM 메타(그대로 프론트에 전달) — 없으면 null */
    private LlmMeta llm;

    /** LLM 라벨 → 해석된 CAT3 코드들(디버깅/표시용) */
    private List<String> cat3FromLlm;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LlmMeta {
        @JsonProperty("schema_version") private Integer schemaVersion;
        @JsonProperty("emotion_diagnosis") private String emotionDiagnosis;
        @JsonProperty("theme_name") private String themeName;
        @JsonProperty("theme_description") private String themeDescription;

        @JsonProperty("category_groups")
        private List<CategoryGroup> categoryGroups;

        private List<Activity> activities;
        private List<String> keywords;

        @JsonProperty("comfort_letter") private String comfortLetter;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
        public static class CategoryGroup {
            @JsonProperty("group_name") private String groupName;
            private List<String> categories;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
        public static class Activity {
            private String title;
            private String description;
        }
    }
}
