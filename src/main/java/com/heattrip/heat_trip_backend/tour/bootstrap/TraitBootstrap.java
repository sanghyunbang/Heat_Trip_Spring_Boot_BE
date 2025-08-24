package com.heattrip.heat_trip_backend.tour.bootstrap;

import com.heattrip.heat_trip_backend.tour.domain.*;
import com.heattrip.heat_trip_backend.tour.repository.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal; // ★ BigDecimal 임포트

/**
 * 앱 시작 시 place_traits/hashtags/simple_tags/descriptions가 "비어있다면" classpath CSV에서 1회 로드
 */
@Component
@RequiredArgsConstructor
public class TraitBootstrap {

    private final PlaceTraitRepo traitRepo;
    private final PlaceHashtagRepo hashtagRepo;
    private final PlaceSimpleTagRepo simpleTagRepo;
    private final PlaceDescriptionRepo descRepo;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void loadOnceIfEmpty() throws Exception {
        if (traitRepo.count() > 0) return; // 이미 있으면 스킵

        var res = new ClassPathResource("data/place_traits.csv");
        try (var is = res.getInputStream();
             var br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            CSVReader reader = new CSVReaderBuilder(br).withSkipLines(1).build(); // 헤더 스킵
            String[] r;
            while ((r = reader.readNext()) != null) {
                // 0:CAT3, 1:이름, 2:P, 3:A, 4:D, 5:sociality, 6:noise, 7:crowdness,
                // 8:del_state, 9:location_type,
                // 10:hash1, 11:hash2, 12:hash3, 13:simple_tag1, 14:simple_tag2, 15:simple_tag3,
                // 16:short_descript1, 17:short_descript2
                String cat3 = safe(r,0);

                // place_traits 본체 (★ BigDecimal 파싱에 bd(...) 사용)
                PlaceTrait t = PlaceTrait.builder()
                        .placeId(cat3)
                        .name(safe(r,1))
                        .pScore(bd(r,2))
                        .aScore(bd(r,3))
                        .dScore(bd(r,4))
                        .sociality(bd(r,5))
                        .noise(bd(r,6))
                        .crowdness(bd(r,7))
                        .build();
                traitRepo.save(t);

                // 해시태그 3개
                saveHashtag(cat3, safe(r,10));
                saveHashtag(cat3, safe(r,11));
                saveHashtag(cat3, safe(r,12));

                // 심플 태그 3개
                saveSimple(cat3, safe(r,13));
                saveSimple(cat3, safe(r,14));
                saveSimple(cat3, safe(r,15));

                // 설명 2개 (CAT3 당 1행)
                PlaceDescription d = PlaceDescription.builder()
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

    // ★ 여기! 이 헬퍼들이 바로 이 파일(부트스트랩 클래스) "아래쪽"에 위치합니다.
    private static String safe(String[] arr, int idx){
        return (idx < arr.length && arr[idx] != null && !arr[idx].isBlank()) ? arr[idx] : null;
    }
    private static BigDecimal bd(String[] arr, int idx){
        try {
            return (idx < arr.length && arr[idx] != null && !arr[idx].isBlank())
                    ? new BigDecimal(arr[idx])
                    : null;
        } catch (Exception ignore) { return null; }
    }
}
