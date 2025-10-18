package com.heattrip.heat_trip_backend.curation.service;

import com.heattrip.heat_trip_backend.curation.dto.CategoryScoreDTO;
import com.heattrip.heat_trip_backend.curation.dto.PadDTO;
import com.heattrip.heat_trip_backend.curation.dto.PlaceScoreDTO;
import com.heattrip.heat_trip_backend.curation.dto.RankRequest;
import com.heattrip.heat_trip_backend.curation.entity.Cat3CategoryMapping;
import com.heattrip.heat_trip_backend.curation.entity.EmotionCategory;
import com.heattrip.heat_trip_backend.curation.entity.Place;
import com.heattrip.heat_trip_backend.curation.entity.PlaceFeatures;
import com.heattrip.heat_trip_backend.curation.repository.Cat3CategoryMappingRepository;
import com.heattrip.heat_trip_backend.curation.repository.CurationPlaceFeaturesRepository;
import com.heattrip.heat_trip_backend.curation.repository.CurationPlaceRepository;
import com.heattrip.heat_trip_backend.curation.repository.EmotionCategoryRepository;
import com.heattrip.heat_trip_backend.curation.repository.PlaceStarRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 스코어링 핵심 로직
 * - 사용자 state+goals → 6축 가중치(w)
 * - feature(−1..1)를 0..1로 변환하여 trait_match 계산
 * - popularity = α*quality(베이지안 평균) + β*volume(로그; n_reviews/n_blogs 사용)
 * - final = 0.6*trait_match + 0.4*popularity
 * - (옵션) CAT3 필터, 거리 가중치 반영
 */
@Service
@RequiredArgsConstructor
public class ScoringService {

    private final CurationPlaceRepository placeRepo;
    private final CurationPlaceFeaturesRepository featuresRepo;
    private final PlaceStarRatingRepository ratingRepo;

    private final EmotionCategoryRepository categoryRepo;
    private final Cat3CategoryMappingRepository mappingRepo;

    // ---- 하이퍼파라미터 ----
    private static final double LAMBDA_G = 0.7, LAMBDA_S = 0.3; // goal vs state
    private static final double W_TRAIT = 0.6, W_POP = 0.4;     // final 결합
    private static final double ALPHA = 0.7, BETA = 0.3;        // popularity: quality vs volume
    private static final double MU0 = 4.2, M = 30.0;            // 베이지안 prior
    private static final int N_REF = 500;                       // 볼륨 정규화 기준

    /** 상위 N 장소 랭킹 (cat3Filter/거리 옵션을 RankRequest에서 읽어 처리) */
    public List<PlaceScoreDTO> rank(RankRequest req) {
        // 1) 사용자 가중치
        Weights w = computeUserWeights(req);

        // 2) 후보 장소 로딩 (CAT3 필터가 있으면 제한)
        Map<Long, Place> places;
        if (req.getCat3Filter() != null && !req.getCat3Filter().isEmpty()) {
            places = placeRepo.findByCat3In(new HashSet<>(req.getCat3Filter()))
                    .stream().collect(Collectors.toMap(Place::getId, x -> x));
        } else {
            places = placeRepo.findAll().stream()
                    .collect(Collectors.toMap(Place::getId, x -> x));
        }
        if (places.isEmpty()) return List.of();

        // 3) feature + 별점 집계
        Map<Long, PlaceFeatures> feats = featuresRepo.findByPlaceIdIn(places.keySet())
                .stream().collect(Collectors.toMap(PlaceFeatures::getPlaceId, x -> x));

        Map<Long, Agg> aggMap = new HashMap<>();
        for (PlaceStarRatingRepository.RatingAgg a : ratingRepo.avgByLinkIds(feats.keySet())) {
            aggMap.put(a.getLinkId(), new Agg(a.getAvgRating(), a.getNRows()));
        }

        // 4) 스코어 계산 (+ 거리 가중치)
        final Double originLat = req.getOriginLat();
        final Double originLon = req.getOriginLon();
        final Double maxDistKm = req.getMaxDistanceKm();
        final double dw = req.getDistanceWeight() == null ? 0.2 : clamp01(req.getDistanceWeight());

        List<PlaceScoreDTO> out = new ArrayList<>();
        for (Map.Entry<Long, PlaceFeatures> e : feats.entrySet()) {
            Long id = e.getKey();
            PlaceFeatures pf = e.getValue();
            Place pl = places.get(id);
            if (pl == null) continue;

            // (a) feature 정규화
            double fSoc = sat01((nz(pf.getSociality())+1)/2.0, pf.getConfSociality());
            double fSpi = sat01((nz(pf.getSpirituality())+1)/2.0, pf.getConfSpirituality());
            double fAdv = sat01((nz(pf.getAdventure())+1)/2.0, pf.getConfAdventure());
            double fCul = sat01((nz(pf.getCulture())+1)/2.0, pf.getConfCulture());
            double fNat = sat01((nz(pf.getNatureHealing())+1)/2.0, pf.getConfNatureHealing());
            double fQui = sat01((nz(pf.getQuiet())+1)/2.0, pf.getConfQuiet());

            // (b) trait_match
            double trait = w.quiet*fQui + w.nature*fNat + w.spirituality*fSpi
                    + w.sociality*fSoc + w.adventure*fAdv + w.culture*fCul;

            // (c) popularity
            Agg agg = aggMap.get(id);
            int nReviews = pf.getNReviews() == null ? 0 : pf.getNReviews();
            int nBlogs   = pf.getNBlogs()   == null ? 0 : pf.getNBlogs();
            double quality = qualityFromAvg(agg == null ? null : agg.avgRating(), nReviews);
            int nEff = Math.max(0, nReviews) + (int)Math.round(0.5 * Math.max(0, nBlogs));
            double volume = Math.log(1 + nEff) / Math.log(1 + N_REF);
            if (volume > 1.0) volume = 1.0;

            double popularity = ALPHA * quality + BETA * volume;
            double baseFinal  = W_TRAIT * trait + W_POP * popularity;

            // (d) 거리 가중치
            Double distanceKm = null;
            Double distanceScore = null;
            if (originLat != null && originLon != null && pl.getMapy() != null && pl.getMapx() != null) {
                distanceKm = haversine(originLat, originLon, pl.getMapy(), pl.getMapx());
                if (maxDistKm != null && distanceKm > maxDistKm) {
                    // 반경 컷: 제외
                    continue;
                }
                if (maxDistKm != null && maxDistKm > 0) {
                    // 가까울수록 1, maxDistKm에서 0 (선형)
                    distanceScore = clamp01(1.0 - (distanceKm / maxDistKm));
                } else {
                    // 반경 미지정이면 완만한 감쇠(100km 기준)
                    double ref = 100.0;
                    distanceScore = 1.0 / (1.0 + (distanceKm / ref));
                }
            }

            double finalScore = baseFinal;
            if (distanceScore != null && dw > 0) {
                finalScore = (1 - dw) * baseFinal + dw * distanceScore;
            }

            out.add(PlaceScoreDTO.builder()
                    .placeId(id)
                    .name(pl.getTitle())
                    .cat3Code(pl.getCat3())
                    .traitMatch(trait)
                    .popularity(popularity)
                    .finalScore(finalScore)
                    .distanceKm(distanceKm)
                    .distanceScore(distanceScore)
                    .build());
        }

        // 5) 정렬/상위 N
        int topN = req.getTopN() != null ? req.getTopN() : 50;
        return out.stream()
                .sorted(Comparator.comparingDouble(PlaceScoreDTO::getFinalScore).reversed())
                .limit(topN)
                .toList();
    }

    /** 카테고리 상위 N 집계 (기존 그대로) */
    @Transactional(readOnly = true)
    public List<CategoryScoreDTO> categories(RankRequest req) {
        List<PlaceScoreDTO> ranked = rank(req);

        Set<String> cat3Set = ranked.stream()
                .map(PlaceScoreDTO::getCat3Code)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<String, Integer> cat3ToCatId = mappingRepo.findByCat3CodeIn(cat3Set)
                .stream().collect(Collectors.toMap(Cat3CategoryMapping::getCat3Code, Cat3CategoryMapping::getCategoryId));

        Map<Integer, List<PlaceScoreDTO>> byCat = new HashMap<>();
        for (PlaceScoreDTO p : ranked) {
            Integer catId = cat3ToCatId.get(p.getCat3Code());
            if (catId == null) continue;
            byCat.computeIfAbsent(catId, k -> new ArrayList<>()).add(p);
        }

        Map<Integer, EmotionCategory> meta = categoryRepo.findAll().stream()
                .collect(Collectors.toMap(EmotionCategory::getId, x -> x));

        List<CategoryScoreDTO> out = new ArrayList<>();
        for (Map.Entry<Integer, List<PlaceScoreDTO>> e : byCat.entrySet()) {
            Integer catId = e.getKey();
            List<PlaceScoreDTO> places = e.getValue();

            List<PlaceScoreDTO> topPlaces = places.stream()
                    .sorted(Comparator.comparingDouble(PlaceScoreDTO::getFinalScore).reversed())
                    .limit(6)
                    .toList();

            double avgFinal = places.stream()
                    .mapToDouble(PlaceScoreDTO::getFinalScore)
                    .average().orElse(0);

            EmotionCategory mc = meta.get(catId);
            out.add(CategoryScoreDTO.builder()
                    .categoryId(catId)
                    .categoryName(mc != null ? mc.getName() : ("CAT#" + catId))
                    .emoji(mc != null ? mc.getEmoji() : "🗺️")
                    .score(avgFinal)
                    .topPlaces(topPlaces)
                    .build());
        }

        int topN = (req.getTopN() != null && req.getTopN() > 0) ? req.getTopN() : 6;
        return out.stream()
                .sorted(Comparator.comparingDouble(CategoryScoreDTO::getScore).reversed())
                .limit(topN)
                .toList();
    }

    // ===== 내부 수학/보조 =====

    private record Agg(Double avgRating, Long nRows) {}

    private static double qualityFromAvg(Double avgRating, int nReviews) {
        double p0 = (MU0 - 1.0) / 4.0;
        double p  = (avgRating == null) ? p0 : (avgRating - 1.0) / 4.0;
        return (M * p0 + nReviews * p) / (M + Math.max(0, nReviews));
    }

    private static double sat01(double f01, Double conf) {
        double x = clamp01(f01);
        if (conf == null) return x;
        double factor = 0.5 + 0.5 * clamp01(conf); // 0.5..1.0
        return clamp01(0.5 + (x - 0.5) * factor);
    }

    private Weights computeUserWeights(RankRequest req) {
        PadDTO pad = req.getPad();
        double P = clamp(pad.getPleasure(), -2, 2) / 2.0;
        double A = clamp(pad.getArousal(),   -2, 2) / 2.0;
        double D = clamp(pad.getDominance(), -2, 2) / 2.0;
        int e = req.getEnergy();
        double s = req.getSocialNeed();

        double posP = Math.max(P,0), negP = Math.max(-P,0);
        double posA = Math.max(A,0), negA = Math.max(-A,0);
        double posD = Math.max(D,0), negD = Math.max(-D,0);

        double ws_quiet  = 0.30*negP + 0.10*posA + 0.05*negD;
        double ws_nature = 0.20*negP;
        double ws_spir   = 0.15*negP + 0.10*negA + 0.05*negD;
        double ws_social = 0.30*s;
        double ws_adv    = 0.15*Math.max(e,0) + 0.10*posA + 0.05*posD;
        double ws_cult   = 0.05*posP;

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

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    private static double nz(Double v) { return v == null ? 0.0 : v; }
    private static double clamp01(double x) { return Math.max(0.0, Math.min(1.0, x)); }
    private static int clamp(int x, int lo, int hi) { return Math.max(lo, Math.min(hi, x)); }

    private static final class Weights {
        final double quiet, nature, spirituality, sociality, adventure, culture;
        Weights(double q, double n, double sp, double so, double ad, double cu) {
            this.quiet = q; this.nature = n; this.spirituality = sp;
            this.sociality = so; this.adventure = ad; this.culture = cu;
        }
    }
}
