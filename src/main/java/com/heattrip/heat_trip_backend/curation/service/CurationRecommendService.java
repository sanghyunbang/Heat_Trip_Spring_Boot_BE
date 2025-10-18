package com.heattrip.heat_trip_backend.curation.service;

import com.heattrip.heat_trip_backend.curation.dto.RankRequest;
import com.heattrip.heat_trip_backend.curation.dto.PlaceScoreDTO;
import com.heattrip.heat_trip_backend.llm.RecommenderClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 1) FastAPI(LLM) 호출
 * 2) LLM 카테고리 라벨 → CAT3 변환
 * 3) RankRequest.cat3Filter 주입 후 ScoringService.rank 호출
 */
@Service
@RequiredArgsConstructor
public class CurationRecommendService {

    private final RecommenderClient recommender;
    private final Cat3DictionaryService cat3Dict;
    private final ScoringService scoring;

    public List<PlaceScoreDTO> recommend(RankRequest in) {
        // 이미 프론트에서 cat3Filter를 준 경우 그대로 사용 (LLM 생략)
        if (in.getCat3Filter() != null && !in.getCat3Filter().isEmpty()) {
            return scoring.rank(in);
        }

        // 1) LLM 호출 (snake_case)
        var llmReq = RecommenderClient.RecommendRequest.builder()
                .pleasure(in.getPad().getPleasure())
                .arousal(in.getPad().getArousal())
                .dominance(in.getPad().getDominance())
                .energy(in.getEnergy())
                .social(in.getSocialNeed())
                .primary_mood(in.getPrimaryMood())
                .purpose_keywords(in.getGoals())
                .emotion_note(in.getEmotionNote())
                .build();
        var res = recommender.recommend(llmReq);

        // 2) 카테고리 라벨 수집 → CAT3 변환
        var labels = res.getCategory_groups().stream()
                .flatMap(g -> g.getCategories().stream())
                .toList();
        Set<String> cat3 = cat3Dict.resolveCat3Codes(labels);

        // 3) 요청에 cat3Filter 주입 후 스코어링
        in.setCat3Filter(cat3.stream().toList());
        return scoring.rank(in);
    }
}
