package com.heattrip.heat_trip_backend.curation.service;

import com.heattrip.heat_trip_backend.curation.dto.*;
import com.heattrip.heat_trip_backend.curation.entity.*;
import com.heattrip.heat_trip_backend.curation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 스코어링 핵심 로직
 * - 사용자 state+goals → 6축 가중치(w)
 * - feature(−1..1)를 0..1로 변환하여 trait_match 계산
 * - popularity = α*quality(베이지안 평균) + β*volume(로그; n_reviews/n_blogs 사용)
 * - final = 0.6*trait_match + 0.4*popularity
 */
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final CurationPlaceRepository placeRepo;
    private final CurationPlaceFeaturesRepository featuresRepo;
    private final PlaceStarRatingRepository ratingRepo;

    // ---- 하이퍼파라미터(튜닝 포인트) ----
    private static final double LAMBDA_G = 0.7, LAMBDA_S = 0.3; // goal vs state
    private static final double W_TRAIT = 0.6, W_POP = 0.4;     // final 결합
    private static final double ALPHA = 0.7, BETA = 0.3;        // popularity: quality vs volume
    private static final double MU0 = 4.2, M = 30.0;            // 베이지안 prior
    private static final int N_REF = 500;                       // 볼륨 정규화 기준

    /** 상위 N 장소 랭킹 */
    public List<PlaceScoreDTO> rank(RankRequest req) {
        // 1) 사용자 가중치
        Weights w = computeUserWeights(req);

        // 2) 엔티티 로딩
        Map<Long, Place> places = placeRepo.findAll().stream()
                .collect(Collectors.toMap(Place::getId, x -> x));
        Map<Long, PlaceFeatures> feats = featuresRepo.findAll().stream()
                .collect(Collectors.toMap(PlaceFeatures::getPlaceId, x -> x));

        // 3) 별점 집계(평균/행수) — link_id 단일 PK라도 AVG/COUNT로 안전 집계
        Map<Long, Agg> aggMap = new HashMap<>();
        for (PlaceStarRatingRepository.RatingAgg a : ratingRepo.avgByLinkIds(feats.keySet())) {
            aggMap.put(a.getLinkId(), new Agg(a.getAvgRating(), a.getNRows()));
        }

        // 4) 스코어 계산
        List<PlaceScoreDTO> out = new ArrayList<>();
        for (Map.Entry<Long, PlaceFeatures> e : feats.entrySet()) {
            Long id = e.getKey();
            PlaceFeatures pf = e.getValue();
            Place pl = places.get(id);
            if (pl == null) continue;

            // (a) feature 정규화(−1..1 → 0..1) + conf 채도 보정
            double fSoc = sat01((nz(pf.getSociality())+1)/2.0, pf.getConfSociality());
            double fSpi = sat01((nz(pf.getSpirituality())+1)/2.0, pf.getConfSpirituality());
            double fAdv = sat01((nz(pf.getAdventure())+1)/2.0, pf.getConfAdventure());
            double fCul = sat01((nz(pf.getCulture())+1)/2.0, pf.getConfCulture());
            double fNat = sat01((nz(pf.getNatureHealing())+1)/2.0, pf.getConfNatureHealing());
            double fQui = sat01((nz(pf.getQuiet())+1)/2.0, pf.getConfQuiet());

            // (b) trait_match: 가중합
            double trait = w.quiet*fQui + w.nature*fNat + w.spirituality*fSpi
                         + w.sociality*fSoc + w.adventure*fAdv + w.culture*fCul;

            // (c) popularity: quality(베이지안) + volume(로그; n_reviews/n_blogs 사용)
            Agg agg = aggMap.get(id);
            int nReviews = pf.getNReviews() == null ? 0 : pf.getNReviews();
            int nBlogs   = pf.getNBlogs()   == null ? 0 : pf.getNBlogs();

            double quality = qualityFromAvg(agg == null ? null : agg.avgRating(), nReviews);
            int nEff = Math.max(0, nReviews) + (int)Math.round(0.5 * Math.max(0, nBlogs));
            double volume = Math.log(1 + nEff) / Math.log(1 + N_REF);
            if (volume > 1.0) volume = 1.0;

            double popularity = ALPHA * quality + BETA * volume;

            // (d) final
            double finalScore = W_TRAIT * trait + W_POP * popularity;

            out.add(PlaceScoreDTO.builder()
                    .placeId(id)
                    .name(pl.getTitle())
                    .cat3Code(pl.getCat3())
                    .traitMatch(trait)
                    .popularity(popularity)
                    .finalScore(finalScore)
                    .build());
        }

        // 5) 정렬/상위 N
        int topN = req.getTopN() != null ? req.getTopN() : 50;
        return out.stream()
                .sorted(Comparator.comparingDouble(PlaceScoreDTO::getFinalScore).reversed())
                .limit(topN)
                .toList();
    }

    // ===== 내부 수학/보조 =====

    private record Agg(Double avgRating, Long nRows) {}

    /** 품질(0..1) — 장소 평균 별점(1..5)을 베이지안 평균으로 안정화 후 0..1 스케일 */
    private static double qualityFromAvg(Double avgRating, int nReviews) {
        double p0 = (MU0 - 1.0) / 4.0;                             // prior 0..1
        double p  = (avgRating == null) ? p0 : (avgRating - 1.0) / 4.0; // 관측 0..1
        return (M * p0 + nReviews * p) / (M + Math.max(0, nReviews));
    }

    /** conf에 따른 채도(saturation) 보정: conf 낮으면 0.5 쪽으로 당김 */
    private static double sat01(double f01, Double conf) {
        double x = clamp01(f01);
        if (conf == null) return x;
        double factor = 0.5 + 0.5 * clamp01(conf); // 0.5..1.0
        return clamp01(0.5 + (x - 0.5) * factor);
    }

    /** 사용자 입력 → 6축 가중치(L1=1) */
    private Weights computeUserWeights(RankRequest req) {
        double P = clamp(req.getPad().getPleasure(), -2, 2) / 2.0;
        double A = clamp(req.getPad().getArousal(),   -2, 2) / 2.0;
        double D = clamp(req.getPad().getDominance(), -2, 2) / 2.0;
        int e = req.getEnergy();
        double s = req.getSocialNeed();

        double posP = Math.max(P,0), negP = Math.max(-P,0);
        double posA = Math.max(A,0), negA = Math.max(-A,0);
        double posD = Math.max(D,0), negD = Math.max(-D,0);

        // state 규칙(선형)
        double ws_quiet  = 0.30*negP + 0.10*posA + 0.05*negD;
        double ws_nature = 0.20*negP;
        double ws_spir   = 0.15*negP + 0.10*negA + 0.05*negD;
        double ws_social = 0.30*s;
        double ws_adv    = 0.15*Math.max(e,0) + 0.10*posA + 0.05*posD;
        double ws_cult   = 0.05*posP;

        // goals 매핑(간단) → 합산 후 L1정규화
        double wg_quiet=0, wg_nat=0, wg_spir=0, wg_soc=0, wg_adv=0, wg_cul=0;
        if (req.getGoals()!=null) for (String g : req.getGoals()) {
            switch (g) {
                case "social"            -> wg_soc  += 1.0;
                case "spiritual"         -> { wg_spir += 1.0; wg_quiet += 0.2; }
                case "adventure"         -> wg_adv  += 1.0;
                case "culture"           -> wg_cul  += 1.0;
                case "nature_healing"    -> { wg_nat += 1.0; wg_quiet += 0.4; }
                case "quiet_reflection"  -> { wg_quiet += 1.0; wg_spir += 0.2; }
            }
        }
        double sum = Math.abs(wg_quiet)+Math.abs(wg_nat)+Math.abs(wg_spir)+Math.abs(wg_soc)+Math.abs(wg_adv)+Math.abs(wg_cul);
        if (sum == 0) sum = 1.0;
        wg_quiet/=sum; wg_nat/=sum; wg_spir/=sum; wg_soc/=sum; wg_adv/=sum; wg_cul/=sum;

        // 혼합
        double q = LAMBDA_G*wg_quiet + LAMBDA_S*ws_quiet;
        double n = LAMBDA_G*wg_nat   + LAMBDA_S*ws_nature;
        double sp= LAMBDA_G*wg_spir  + LAMBDA_S*ws_spir;
        double so= LAMBDA_G*wg_soc   + LAMBDA_S*ws_social;
        double ad= LAMBDA_G*wg_adv   + LAMBDA_S*ws_adv;
        double cu= LAMBDA_G*wg_cul   + LAMBDA_S*ws_cult;

        double l1 = Math.abs(q)+Math.abs(n)+Math.abs(sp)+Math.abs(so)+Math.abs(ad)+Math.abs(cu);
        if (l1 == 0) l1 = 1.0;
        return new Weights(q/l1, n/l1, sp/l1, so/l1, ad/l1, cu/l1);
    }

    // 내부 유틸
    private static double nz(Double v) { return v == null ? 0.0 : v; }
    private static double clamp01(double x) { return Math.max(0.0, Math.min(1.0, x)); }
    private static int clamp(int x, int lo, int hi) { return Math.max(lo, Math.min(hi, x)); }

    /** 6축 가중치 보관 구조체 */
    private static final class Weights {
        final double quiet, nature, spirituality, sociality, adventure, culture;
        Weights(double q, double n, double sp, double so, double ad, double cu) {
            this.quiet = q; this.nature = n; this.spirituality = sp;
            this.sociality = so; this.adventure = ad; this.culture = cu;
        }
    }
}
