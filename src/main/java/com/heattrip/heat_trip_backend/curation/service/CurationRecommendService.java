// com.heattrip.heat_trip_backend.curation.service.CurationRecommendService
package com.heattrip.heat_trip_backend.curation.service;

import com.heattrip.heat_trip_backend.curation.dto.PlaceScoreDTO;
import com.heattrip.heat_trip_backend.curation.dto.RankRequest;
import com.heattrip.heat_trip_backend.curation.dto.RecommendResultDTO;
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

    public RecommendResultDTO recommend(RankRequest in) {

        // 1) cat3Filter가 있으면 LLM 생략 (llm 메타는 null)
        if (in.getCat3Filter()!=null && !in.getCat3Filter().isEmpty()) {
            log.info("Skip LLM: client provided cat3Filter(size={})", in.getCat3Filter().size());
            List<PlaceScoreDTO> ranked = scoring.rank(in);
            return RecommendResultDTO.builder()
                    .places(ranked)
                    .llm(null)
                    .cat3FromLlm(null)
                    .build();
        }

        // 2) LLM 호출
        var llmReq = RecommenderClient.RecommendRequest.builder()
                .pleasure(in.getPad().getPleasure())
                .arousal(in.getPad().getArousal())
                .dominance(in.getPad().getDominance())
                .energy(in.getEnergy())
                .social(in.getSocialNeed())
                .primaryMood(in.getMoodKey())
                .purposeKeywords(in.getGoals())
                .emotionNote(in.getNotes())
                .build();

        var res = recommender.recommend(llmReq); // LLM 응답

        // 3) 라벨 → CAT3
        var labels = res.getCategoryGroups().stream()
                .flatMap(g -> g.getCategories().stream())
                .toList();
        Set<String> cat3 = cat3Dict.resolveCat3Codes(labels);
        in.setCat3Filter(cat3.stream().toList());

        // 4) 랭킹 계산
        List<PlaceScoreDTO> ranked = scoring.rank(in);

        // 5) LLM 메타 구성 + 반환
        var llmMeta = RecommendResultDTO.LlmMeta.builder()
                .schemaVersion(res.getSchemaVersion())
                .emotionDiagnosis(res.getEmotionDiagnosis())
                .themeName(res.getThemeName())
                .themeDescription(res.getThemeDescription())
                .categoryGroups(res.getCategoryGroups().stream()
                        .map(g -> RecommendResultDTO.LlmMeta.CategoryGroup.builder()
                                .groupName(g.getGroupName())
                                .categories(g.getCategories())
                                .build())
                        .toList())
                .activities(res.getActivities().stream()
                        .map(a -> RecommendResultDTO.LlmMeta.Activity.builder()
                                .title(a.getTitle())
                                .description(a.getDescription())
                                .build())
                        .toList())
                .keywords(res.getKeywords())
                .comfortLetter(res.getComfortLetter())
                .build();

        return RecommendResultDTO.builder()
                .places(ranked)
                .llm(llmMeta)
                .cat3FromLlm(in.getCat3Filter())
                .build();
    }
}
