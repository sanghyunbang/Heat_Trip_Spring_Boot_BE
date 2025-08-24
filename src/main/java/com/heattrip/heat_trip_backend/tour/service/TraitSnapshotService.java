// src/main/java/com/heattrip/heat_trip_backend/tour/service/TraitSnapshotService.java
package com.heattrip.heat_trip_backend.tour.service;

import com.heattrip.heat_trip_backend.tour.domain.*;
import com.heattrip.heat_trip_backend.tour.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 수집이 끝난 뒤, places ↔ CAT3 특성/태그/설명을 일괄 조인하여
 * "스냅샷 테이블"을 생성/교체하는 서비스.
 *
 * - FK를 쓰지 않고 값 복제 캐시를 만든다.
 * - @Transactional로 전체를 감싸, 중간 실패 시 롤백 → 이전 스냅샷이 유지.
 */
@Service
@RequiredArgsConstructor
public class TraitSnapshotService {

    private final PlaceRepository placeRepo;               // 이미 존재
    private final PlaceTraitRepo traitRepo;
    private final PlaceHashtagRepo hashtagRepo;
    private final PlaceSimpleTagRepo simpleTagRepo;
    private final PlaceDescriptionRepo descRepo;
    private final PlaceTraitSnapshotRepo snapshotRepo;

    /**
     * [권장] 전량 재빌드 방식.
     * - 간단하고 일관성 보장 용이.
     * - 규모가 매우 클 경우 pageSize를 적절히 조절.
     */
    @Transactional
    public void rebuildAllSnapshots(int pageSize) {
        // 1) 기존 스냅샷 전부 삭제 (트랜잭션 안에서 수행 → 실패 시 롤백)
        snapshotRepo.deleteAllInBatch();

        // 2) places 전체를 페이지로 훑으며 스냅샷 생성
        int page = 0;
        while (true) {
            Page<Place> p = placeRepo.findAll(PageRequest.of(page, pageSize));
            if (!p.hasContent()) break;
            List<Place> places = p.getContent();

            // 이번 청크에서 필요한 CAT3 모음
            Set<String> cat3s = places.stream()
                    .map(Place::getCat3)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // CAT3 → 특성/태그/설명 맵 로딩
            Map<String, PlaceTrait> traitMap = traitRepo.findAllById(cat3s).stream()
                    .collect(Collectors.toMap(PlaceTrait::getPlaceId, Function.identity()));

            Map<String, List<String>> hashMap = hashtagRepo.findByPlaceIdIn(cat3s).stream()
                    .collect(Collectors.groupingBy(
                            PlaceHashtag::getPlaceId,
                            Collectors.mapping(PlaceHashtag::getHashtag, Collectors.toList())
                    ));

            Map<String, List<String>> simpleMap = simpleTagRepo.findByPlaceIdIn(cat3s).stream()
                    .collect(Collectors.groupingBy(
                            PlaceSimpleTag::getPlaceId,
                            Collectors.mapping(PlaceSimpleTag::getTag, Collectors.toList())
                    ));

            Map<String, PlaceDescription> descMap = descRepo.findByPlaceIdIn(cat3s).stream()
                    .collect(Collectors.toMap(PlaceDescription::getPlaceId, Function.identity()));

            LocalDateTime now = LocalDateTime.now();

            // 스냅샷 생성
            List<PlaceTraitSnapshot> snaps = new ArrayList<>(places.size());
            for (Place pl : places) {
                String cat3 = pl.getCat3();
                PlaceTrait t = cat3 == null ? null : traitMap.get(cat3);
                PlaceDescription d = cat3 == null ? null : descMap.get(cat3);

                    snaps.add(PlaceTraitSnapshot.builder()
                        .contentId(pl.getContentid())
                        .cat3(cat3)
                        .cat3Name(t == null ? null : t.getName())
                        .pScore(t == null || t.getPScore()==null ? null : t.getPScore().doubleValue())
                        .aScore(t == null || t.getAScore()==null ? null : t.getAScore().doubleValue())
                        .dScore(t == null || t.getDScore()==null ? null : t.getDScore().doubleValue())
                        .sociality(t == null || t.getSociality()==null ? null : t.getSociality().doubleValue())
                        .noise(t == null || t.getNoise()==null ? null : t.getNoise().doubleValue())
                        .crowdness(t == null || t.getCrowdness()==null ? null : t.getCrowdness().doubleValue())
                        .hashtags(hashMap.getOrDefault(cat3, List.of()))
                        .simpleTags(simpleMap.getOrDefault(cat3, List.of()))
                        .shortDesc1(d == null ? null : d.getShortDesc1())
                        .shortDesc2(d == null ? null : d.getShortDesc2())
                        .snapshotAt(now)
                        .build());
            }

            // 배치 저장 (JPA batch 옵션 활용)
            snapshotRepo.saveAll(snaps);

            page++;
            if (p.isLast()) break;
        }
    }
}
