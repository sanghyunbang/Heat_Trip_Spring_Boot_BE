package com.heattrip.heat_trip_backend.S3.media;

import com.heattrip.heat_trip_backend.S3.UploadCategory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 범용 미디어 API.
 * - 어떤 도메인에서도 사용 가능한 업로드/읽기/교체/삭제 엔드포인트
 * - 엔티티 변경( key → objectKey / 컬럼: media_key ) 반영
 */
@RestController
@RequestMapping("/media")
@Validated // @Min/@Max 등 메서드 파라미터 검증 활성화
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }


    // ─────────────────────────────────────────────────────────────────────
    // C: 멀티 파일 업로드
    // ─────────────────────────────────────────────────────────────────────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> upload(
        @RequestPart("files") List<MultipartFile> files,
        @RequestParam(name = "category") UploadCategory category, // ← name 명시
        @RequestParam(name = "refType", required = false) String refType,
        @RequestParam(name = "refId",   required = false) String refId,
        @AuthenticationPrincipal String userId
    ) {
        var saved = mediaService.uploadMany(files, category, userId, refType, refId);

        // 응답 JSON 필드명은 기존과 동일하게 "key" 유지(클라이언트 호환)
        var body = saved.stream().map(m -> Map.<String, Object>of(
            "id", m.getId(),
            "key", m.getObjectKey(),
            "url", mediaService.publicUrl(m.getObjectKey()),
            "contentType", m.getContentType(),
            "size", m.getSize(),
            "category", m.getCategory().name(),
            "refType", m.getRefType(),
            "refId", m.getRefId()
        )).toList();

        return ResponseEntity.ok(body);
    }

    // ─────────────────────────────────────────────────────────────────────
    // R: 공개 URL 조립 (공개 배포)
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping(path = "/url", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getPublicUrl(
        @RequestParam(name = "key") String objectKey // 파라미터 이름은 호환성을 위해 key 유지
    ) {
        return ResponseEntity.ok(Map.of("url", mediaService.publicUrl(objectKey)));
    }

    // ─────────────────────────────────────────────────────────────────────
    // R: 프리사인드 URL 발급 (비공개 배포)
    // ─────────────────────────────────────────────────────────────────────
    @GetMapping(path = "/presigned", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getPresigned(
        @RequestParam(name = "key") String objectKey,
        @RequestParam(name = "minutes", defaultValue = "10") @Min(1) @Max(120) int minutes
    ) {
        var url = mediaService.presignedUrl(objectKey, Duration.ofMinutes(minutes));
        return ResponseEntity.ok(Map.of("url", url));
    }

    // ─────────────────────────────────────────────────────────────────────
    // U: 파일 교체
    // ─────────────────────────────────────────────────────────────────────
    @PutMapping(path = "/{mediaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> replace(
        @PathVariable(name = "mediaId") @Min(1) Long mediaId, // ← name 명시
        @RequestPart("file") MultipartFile file,
        @AuthenticationPrincipal String userId
    ) {
        var m = mediaService.replace(mediaId, file, userId);
        return ResponseEntity.ok(Map.of(
            "id", m.getId(),
            "key", m.getObjectKey(),
            "url", mediaService.publicUrl(m.getObjectKey())
        ));
    }

    // ─────────────────────────────────────────────────────────────────────
    // D: 단건 삭제
    // ─────────────────────────────────────────────────────────────────────
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> delete(
        @PathVariable(name = "mediaId") @Min(1) Long mediaId, // ← name 명시
        @AuthenticationPrincipal String userId
    ) {
        mediaService.delete(mediaId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // D: 연결 리소스 전체 삭제 (refType+refId 기준)
    // ─────────────────────────────────────────────────────────────────────
    @DeleteMapping
    public ResponseEntity<Void> deleteByRef(
        @RequestParam(name = "refType") String refType, // ← name 명시
        @RequestParam(name = "refId")   String refId,   // ← name 명시
        @AuthenticationPrincipal String userId
    ) {
        mediaService.deleteByRef(refType, refId, userId);
        return ResponseEntity.noContent().build();
    }
}
