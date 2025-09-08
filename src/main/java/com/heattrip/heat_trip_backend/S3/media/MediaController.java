package com.heattrip.heat_trip_backend.S3.media;

import com.heattrip.heat_trip_backend.S3.UploadCategory;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 범용 미디어 API.
 * - 어떤 도메인에서도 사용 가능한 업로드/읽기/교체/삭제 엔드포인트
 */
@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /** 프로젝트 공통 JWT 유틸로 대체하세요. 여기선 단순 샘플 */
    private String ownerId(HttpServletRequest req) {
        Object v = req.getAttribute("ownerId");
        if (v == null) throw new IllegalStateException("ownerId가 누락되었습니다.");
        return v.toString();
    }

    /** C: 멀티 파일 업로드 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
        @RequestPart("files") List<MultipartFile> files,
        @RequestParam UploadCategory category,               // JOURNEY_IMAGE / PROFILE_IMAGE / REVIEW_IMAGE
        @RequestParam(required = false) String refType,      // 예: "JOURNEY","REVIEW","PROFILE"
        @RequestParam(required = false) String refId,        // 예: journeyId, reviewId
        HttpServletRequest req
    ) {
        String owner = ownerId(req);
        var saved = mediaService.uploadMany(files, category, owner, refType, refId);
        var body = saved.stream().map(m -> Map.of(
            "id", m.getId(),
            "key", m.getKey(),
            "url", mediaService.publicUrl(m.getKey()),
            "contentType", m.getContentType(),
            "size", m.getSize(),
            "category", m.getCategory().name(),
            "refType", m.getRefType(),
            "refId", m.getRefId()
        )).toList();
        return ResponseEntity.ok(body);
    }

    /** R: 공개 URL 조립 (공개 배포) */
    @GetMapping("/url")
    public ResponseEntity<?> getPublicUrl(@RequestParam String key) {
        return ResponseEntity.ok(Map.of("url", mediaService.publicUrl(key)));
    }

    /** R: 프리사인드 URL 발급 (비공개 배포) */
    @GetMapping("/presigned")
    public ResponseEntity<?> getPresigned(
        @RequestParam String key,
        @RequestParam(defaultValue = "10") int minutes
    ) {
        return ResponseEntity.ok(Map.of("url", mediaService.presignedUrl(key, Duration.ofMinutes(minutes))));
    }

    /** U: 파일 교체 */
    @PutMapping(path = "/{mediaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> replace(
        @PathVariable Long mediaId,
        @RequestPart("file") MultipartFile file,
        HttpServletRequest req
    ) {
        String owner = ownerId(req);
        var m = mediaService.replace(mediaId, file, owner);
        return ResponseEntity.ok(Map.of(
            "id", m.getId(),
            "key", m.getKey(),
            "url", mediaService.publicUrl(m.getKey())
        ));
    }

    /** D: 단건 삭제 */
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<?> delete(@PathVariable Long mediaId, HttpServletRequest req) {
        String owner = ownerId(req);
        mediaService.delete(mediaId, owner);
        return ResponseEntity.noContent().build();
    }

    /** D: 연결 리소스 전체 삭제 */
    @DeleteMapping
    public ResponseEntity<?> deleteByRef(
        @RequestParam String refType,
        @RequestParam String refId,
        HttpServletRequest req
    ) {
        String owner = ownerId(req);
        mediaService.deleteByRef(refType, refId, owner);
        return ResponseEntity.noContent().build();
    }
}
