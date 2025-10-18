package com.heattrip.heat_trip_backend.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * FastAPI(LLM) /recommend 호출
 */
@Component
public class RecommenderClient {

    private final WebClient webClient;

    public RecommenderClient(
            @Value("${llm.recommender.base-url}") String baseUrl,
            @Value("${llm.recommender.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${llm.recommender.read-timeout-ms:5000}") long readTimeoutMs
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public RecommendResponse recommend(RecommendRequest req) {
        return webClient.post()
                .uri("/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(RecommendResponse.class)
                .timeout(Duration.ofMillis(7000))
                .retryWhen(Retry.backoff(1, Duration.ofMillis(200)))
                .block();
    }

    @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecommendRequest {
        private double pleasure;
        private double arousal;
        private double dominance;
        private double energy;
        private double social;
        private String primary_mood;
        private List<String> purpose_keywords;
        private String emotion_note;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RecommendResponse {
        private int schema_version;
        private String emotion_diagnosis;
        private String theme_name;
        private String theme_description;
        private List<CategoryGroup> category_groups;
        private List<Activity> activities;
        private List<String> keywords;
        private String comfort_letter;

        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        public static class CategoryGroup {
            private String group_name;
            private List<String> categories;
        }
        @Getter @Setter @NoArgsConstructor @AllArgsConstructor
        public static class Activity {
            private String title;
            private String description;
        }
    }
}
