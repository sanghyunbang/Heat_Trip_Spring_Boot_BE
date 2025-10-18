// src/main/java/com/heattrip/heat_trip_backend/curation/service/CurationRecommendService.java
package com.heattrip.heat_trip_backend.curation.service;

import com.heattrip.heat_trip_backend.curation.dto.RankRequest;
import com.heattrip.heat_trip_backend.curation.dto.PlaceScoreDTO;
import com.heattrip.heat_trip_backend.llm.RecommenderClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurationRecommendService {

    private final RecommenderClient recommender;
    private final Cat3DictionaryService cat3Dict;
    private final ScoringService scoring;

    public List<PlaceScoreDTO> recommend(RankRequest in) {
        log.info("[/recommend] topN={}, goals={}, cat3Filter={}",
                in.getTopN(), in.getGoals(), in.getCat3Filter());

        // 0) 프런트가 cat3Filter를 준 경우: LLM 생략
        if (in.getCat3Filter() != null && !in.getCat3Filter().isEmpty()) {
            log.info("Skip LLM: client provided cat3Filter(size={})", in.getCat3Filter().size());
            return scoring.rank(in);
        }

        // 1) LLM 요청 빌드 (camelCase → @JsonProperty로 snake_case 직렬화됨)
        var llmReq = RecommenderClient.RecommendRequest.builder()
                .pleasure(in.getPad().getPleasure())
                .arousal(in.getPad().getArousal())
                .dominance(in.getPad().getDominance())
                .energy(in.getEnergy())
                .social(in.getSocialNeed())
                .primaryMood(in.getPrimaryMood())
                .purposeKeywords(in.getGoals())
                .emotionNote(in.getEmotionNote())
                .build();

        var res = recommender.recommend(llmReq);
        log.info("LLM theme='{}', groups={}",
                res.getThemeName(),
                res.getCategoryGroups() == null ? 0 : res.getCategoryGroups().size());

        // 2) 라벨 → CAT3
        var labels = res.getCategoryGroups().stream()
                .flatMap(g -> g.getCategories().stream())
                .toList();
        log.info("LLM labels={}", labels);

        Set<String> cat3 = cat3Dict.resolveCat3Codes(labels);
        log.info("Resolved CAT3 codes(size={}): {}", cat3.size(), cat3);

        if (cat3.isEmpty()) {
            log.warn("No CAT3 resolved from labels. Ranking will likely be empty.");
        }

        // 3) cat3Filter 주입 후 스코어링
        in.setCat3Filter(cat3.stream().toList());
        return scoring.rank(in);
    }
}
