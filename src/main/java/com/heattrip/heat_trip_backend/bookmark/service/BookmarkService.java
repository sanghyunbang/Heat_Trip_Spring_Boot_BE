// src/main/java/com/heattrip/heat_trip_backend/bookmark/service/BookmarkService.java
package com.heattrip.heat_trip_backend.bookmark.service;

import com.heattrip.heat_trip_backend.bookmark.entity.Bookmark;
import com.heattrip.heat_trip_backend.bookmark.repository.BookmarkRepository;
import com.heattrip.heat_trip_backend.bookmark.repository.PlaceImgRepository;
import com.heattrip.heat_trip_backend.bookmark.service.CollectionService;

import com.heattrip.heat_trip_backend.tour.domain.Place;
import com.heattrip.heat_trip_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final CollectionService collectionService;
    private final PlaceImgRepository placeImgRepository;

    /** 내 북마크 전체 */
    public List<Bookmark> findAll(User user) {
        return bookmarkRepository.findByUser(user);
    }

    /** 북마크 추가 (중복 방지) + 선택적으로 컬렉션에도 추가 */
    @Transactional
    public void add(User user, String contentId, String collectionIdOrNull) {
        bookmarkRepository.findByUserAndContentId(user, contentId)
                .orElseGet(() -> bookmarkRepository.save(
                        Bookmark.builder().user(user).contentId(contentId).build()
                ));
        if (collectionIdOrNull != null && !collectionIdOrNull.isBlank()) {
            collectionService.addItem(user, collectionIdOrNull, contentId);
        }
    }

    /** 북마크 제거 (컬렉션 아이템은 유지) */
    @Transactional
    public void remove(User user, String contentId) {
        bookmarkRepository.deleteByUserAndContentId(user, contentId);
    }

    /* -------------------- 이미지 조회 -------------------- */

    /** 문자열 contentId → Long 파싱 후 Place 조회 */
    public Optional<Place> findByContentId(String contentId) {
        try {
            long id = Long.parseLong(contentId);
            // Place의 @Id가 contentid 이므로 findById 사용
            return placeImgRepository.findById(id);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** 배치: 여러 contentId → firstimage 맵(contentId → url) */
    public Map<String, String> findImagesByContentIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();

        // String → Long (파싱 실패 제거)
        List<Long> longIds = ids.stream().map(s -> {
            try { return Long.parseLong(s); } catch (Exception e) { return null; }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (longIds.isEmpty()) return Map.of();

        // 한 번에 조회
        List<Place> rows = placeImgRepository.findAllById(longIds);

        Map<String, String> out = new HashMap<>();
        for (Place p : rows) {
            String url = preferFirstimage(p);
            if (url != null && !url.isBlank()) {
                out.put(String.valueOf(p.getContentid()), url);
            }
        }
        return out;
    }

    private String preferFirstimage(Place p) {
        if (p.getFirstimage() != null && !p.getFirstimage().isBlank()) return p.getFirstimage();
        if (p.getFirstimage2() != null && !p.getFirstimage2().isBlank()) return p.getFirstimage2();
        return null;
    }
}
