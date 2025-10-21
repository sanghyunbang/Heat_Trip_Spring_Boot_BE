// src/main/java/com/heattrip/heat_trip_backend/llm/RecommenderClient.java
package com.heattrip.heat_trip_backend.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommenderClient {

    private final @Qualifier("llmWebClient") WebClient llmWebClient;

    /** FastAPI /recommend 호출 */
    public RecommendResponse recommend(RecommendRequest req) {
        log.info("[LLM] POST /recommend");
        return llmWebClient.post()
                .uri("/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.body(BodyExtractors.toMono(String.class))
                                .defaultIfEmpty("")
                                .flatMap(body -> {
                                    log.error("[LLM] HTTP {} body={}", resp.statusCode(), body);
                                    return Mono.error(new RuntimeException("LLM HTTP " + resp.statusCode().value()));
                                })
                )
                .bodyToMono(RecommendResponse.class)
                .timeout(Duration.ofSeconds(75)) // 파이프라인 전체 타임아웃
                .retryWhen(Retry.backoff(2, Duration.ofMillis(300))
                        .filter(ex -> !(ex instanceof WebClientResponseException))) // 4xx는 재시도 안함
                .doOnError(e -> log.error("[LLM] call failed: {}", e.toString()))
                .block();
    }

    // ====== DTOs ======
    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecommendRequest {
        private double pleasure;
        private double arousal;
        private double dominance;
        private double energy;
        private double social;

        // ★ 파이썬/스프링/플러터 통일: moodKey / notes
        @JsonProperty("moodKey")
        private String moodKey;

        // 목적 키워드는 파이썬이 snake_case로 받음 (유지)
        @JsonProperty("purpose_keywords")
        private List<String> purposeKeywords;

        @JsonProperty("notes")
        private String notes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RecommendResponse {
        @JsonProperty("schema_version")
        private int schemaVersion;

        @JsonProperty("emotion_diagnosis")
        private String emotionDiagnosis;

        @JsonProperty("theme_name")
        private String themeName;

        @JsonProperty("theme_description")
        private String themeDescription;

        @JsonProperty("category_groups")
        private List<CategoryGroup> categoryGroups;

        private List<Activity> activities;
        private List<String> keywords;

        @JsonProperty("comfort_letter")
        private String comfortLetter;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        public static class CategoryGroup {
            @JsonProperty("group_name")
            private String groupName;
            private List<String> categories;
        }

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        public static class Activity {
            private String title;
            private String description;
        }
    }
}
