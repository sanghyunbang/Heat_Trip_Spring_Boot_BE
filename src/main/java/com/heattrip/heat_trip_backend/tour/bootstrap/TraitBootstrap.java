// src/main/java/com/heattrip/heat_trip_backend/tour/bootstrap/TraitBootstrap.java
package com.heattrip.heat_trip_backend.tour.bootstrap;

import com.heattrip.heat_trip_backend.curation.entity.PlaceTrait; // ★ curation 엔티티 사용
import com.heattrip.heat_trip_backend.curation.repository.PlaceTraitRepository; // ★ curation 리포지토리 사용
import com.heattrip.heat_trip_backend.tour.domain.PlaceHashtag;
import com.heattrip.heat_trip_backend.tour.domain.PlaceSimpleTag;
import com.heattrip.heat_trip_backend.tour.domain.PlaceDescription;
import com.heattrip.heat_trip_backend.tour.repository.PlaceHashtagRepo;
import com.heattrip.heat_trip_backend.tour.repository.PlaceSimpleTagRepo;
import com.heattrip.heat_trip_backend.tour.repository.PlaceDescriptionRepo;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraitBootstrap {

    private final PlaceTraitRepository traitRepo; // ★ 변경: curation 리포
    private final PlaceHashtagRepo hashtagRepo;
    private final PlaceSimpleTagRepo simpleTagRepo;
    private final PlaceDescriptionRepo descRepo;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadOnceIfEmpty() throws Exception {
        if (traitRepo.count() > 0) return;

        var res = new ClassPathResource("data/place_traits.csv");
        if (!res.exists()) {
            // 시드 데이터(data/** 는 .gitignore 대상)가 배포되지 않은 환경(테스트/클린 부팅)에서는
            // 예외로 부팅을 막지 말고 스킵한다.
            log.warn("[TraitBootstrap] data/place_traits.csv 가 없어 트레이트 시드를 건너뜁니다.");
            return;
        }
        try (var is = res.getInputStream();
             var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            CSVReader reader = new CSVReaderBuilder(br).withSkipLines(1).build();
            String[] r;
            while ((r = reader.readNext()) != null) {
                String cat3 = safe(r,0);

                PlaceTrait t = PlaceTrait.builder()
                        .placeId(cat3)
                        .name(safe(r,1))
                        .pScore(dbl(r,2))
                        .aScore(dbl(r,3))
                        .dScore(dbl(r,4))
                        .sociality(dbl(r,5))
                        .noise(dbl(r,6))
                        .crowdness(dbl(r,7))
                        .delState(intOrNull(r,8))
                        .locationType(safe(r,9))
                        .hash1(safe(r,10)).hash2(safe(r,11)).hash3(safe(r,12))
                        .simpleTag1(safe(r,13)).simpleTag2(safe(r,14)).simpleTag3(safe(r,15))
                        .shortDescript1(safe(r,16)).shortDescript2(safe(r,17))
                        .build();
                traitRepo.save(t);

                saveHashtag(cat3, safe(r,10));
                saveHashtag(cat3, safe(r,11));
                saveHashtag(cat3, safe(r,12));

                saveSimple(cat3, safe(r,13));
                saveSimple(cat3, safe(r,14));
                saveSimple(cat3, safe(r,15));

                var d = PlaceDescription.builder()
                        .placeId(cat3)
                        .shortDesc1(safe(r,16))
                        .shortDesc2(safe(r,17))
                        .build();
                descRepo.save(d);
            }
        }
    }

    private void saveHashtag(String cat3, String v){
        if (v!=null && !v.isBlank()) {
            hashtagRepo.save(PlaceHashtag.builder().placeId(cat3).hashtag(v).build());
        }
    }
    private void saveSimple(String cat3, String v){
        if (v!=null && !v.isBlank()) {
            simpleTagRepo.save(PlaceSimpleTag.builder().placeId(cat3).tag(v).build());
        }
    }

    private static String safe(String[] arr, int idx){
        return (idx < arr.length && arr[idx] != null && !arr[idx].isBlank()) ? arr[idx] : null;
    }
    private static Double dbl(String[] arr, int idx){
        try {
            return (idx < arr.length && arr[idx] != null && !arr[idx].isBlank())
                    ? Double.valueOf(arr[idx])
                    : null;
        } catch (Exception ignore) { return null; }
    }
    private static Integer intOrNull(String[] arr, int idx){
        try {
            return (idx < arr.length && arr[idx] != null && !arr[idx].isBlank())
                    ? Integer.valueOf(arr[idx])
                    : null;
        } catch (Exception ignore) { return null; }
    }
}
