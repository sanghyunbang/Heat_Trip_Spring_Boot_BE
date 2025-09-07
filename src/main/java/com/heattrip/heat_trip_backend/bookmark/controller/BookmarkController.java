// src/main/java/com/heattrip/heat_trip_backend/bookmark/controller/BookmarkController.java
package com.heattrip.heat_trip_backend.bookmark.controller;

import com.heattrip.heat_trip_backend.bookmark.dto.BookmarkRequest;
import com.heattrip.heat_trip_backend.bookmark.dto.BookmarkResponse;
import com.heattrip.heat_trip_backend.bookmark.entity.Bookmark;
import com.heattrip.heat_trip_backend.bookmark.service.BookmarkService;
import com.heattrip.heat_trip_backend.user.entity.User;
import com.heattrip.heat_trip_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final UserService userService;

    private User me(String authHeader) {
        String email = userService.emailFromAuth(authHeader);
        return userService.findByEmail(email);
    }

    /** 내 북마크 목록 */
    @GetMapping
    public ResponseEntity<List<BookmarkResponse>> list(@RequestHeader("Authorization") String authHeader) {
        User user = me(authHeader);
        List<Bookmark> rows = bookmarkService.findAll(user);
        return ResponseEntity.ok(rows.stream().map(BookmarkResponse::from).toList());
    }

    /** 북마크 추가 (collectionId는 선택) */
    @PostMapping
    public ResponseEntity<Void> add(@RequestHeader("Authorization") String authHeader,
                                    @RequestBody BookmarkRequest req) {
        if (req.getContentId() == null || req.getContentId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        User user = me(authHeader);
        bookmarkService.add(user, req.getContentId(), req.getCollectionId());
        return ResponseEntity.ok().build();
    }

    /** 북마크 제거 */
    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> remove(@RequestHeader("Authorization") String authHeader,
                                       @PathVariable String contentId) {
        User user = me(authHeader);
        bookmarkService.remove(user, contentId);
        return ResponseEntity.noContent().build();
    }

    /* -------- 이미지: 단건/배치 -------- */

    /** 단건: /bookmarks/img/{contentId} → { contentId, firstimage, imageUrl } */
    @GetMapping("/img/{contentId}")
    public ResponseEntity<Map<String, Object>> getOne(@PathVariable String contentId) {
        return ResponseEntity.of(
                bookmarkService.findByContentId(contentId)
                        .map(p -> {
                            String first = p.getFirstimage();
                            String second = p.getFirstimage2();
                            String imageUrl = (first != null && !first.isBlank()) ? first : (second == null ? "" : second);

                            return Map.<String, Object>of(
                                    "contentId", String.valueOf(p.getContentid()),
                                    "firstimage", first == null ? "" : first,
                                    "imageUrl", imageUrl
                            );
                        })
        ); // Optional → 200/404 자동 처리
    }

    /** 배치: POST /bookmarks/images:batchResolve { contentIds: [...] } */
    @PostMapping("/images:batchResolve")
    public ResponseEntity<List<Map<String, String>>> batch(@RequestBody Map<String, Object> req) {
        @SuppressWarnings("unchecked")
        List<Object> raw = (List<Object>) req.getOrDefault("contentIds", List.of());
        List<String> ids = raw.stream().map(String::valueOf).toList();

        Map<String, String> map = bookmarkService.findImagesByContentIds(ids);

        // 요청 순서를 유지해 응답
        List<Map<String, String>> body = new ArrayList<>();
        for (String id : ids) {
            String url = map.get(id);
            if (url != null && !url.isBlank()) {
                body.add(Map.of("contentId", id, "imageUrl", url));
            }
        }
        return ResponseEntity.ok(body);
    }
}
