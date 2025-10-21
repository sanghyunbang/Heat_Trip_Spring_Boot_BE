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

        // ⚠ 기존 문제: 파이프라인 전체 타임아웃(75s)이 소켓 읽기/쓰기(30s/15s)보다 길어서, 소켓 타임아웃이 먼저 터짐
        // ✔ 대응: WebClientConfig에서 소켓/응답을 넉넉히(120s)로 맞췄고,
        //         여기 전체 타임아웃은 180s(또는 제거)로 "가장 크게" 잡아 상위 레이어가 먼저 끊지 않도록 함.
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
                .timeout(Duration.ofSeconds(180)) // (기존 75s → 180s) 전체 파이프라인 상한: 소켓/응답(120s)보다 반드시 길게
                .retryWhen(Retry.backoff(2, Duration.ofMillis(300))
                        // 4xx는 재시도 안 함. 5xx/네트워크 이슈만 재시도
                        .filter(ex -> !(ex instanceof WebClientResponseException)
                                || ((WebClientResponseException) ex).getStatusCode().is5xxServerError())
                )
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
