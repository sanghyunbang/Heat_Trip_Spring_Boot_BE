package com.heattrip.heat_trip_backend.S3;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

/**
 * S3 업로드 전용 서비스.
 *
 * [개념 요약]
 * - S3는 '폴더'가 아니라 '키(key)'라는 문자열로 객체를 식별합니다. [1]
 * - CloudFront는 S3 객체를 읽어 사용자에게 전달하는 CDN입니다. [2]
 * - 우리는 "키 설계 + 검증 + 메타데이터"를 표준화하여 안전하고 운영 가능한 업로드를 만듭니다. [3]
 *
 * [URL/키 매핑 예시]
 *   key   : journeys/42/2025/09/4a0a...-be6a_trip_photo.jpg
 *   S3 URL: https://heattrip-bucket1.s3.ap-northeast-2.amazonaws.com/journeys/42/2025/09/4a0a...-be6a_trip_photo.jpg
 *   CF URL: https://dxxxxx.cloudfront.net/journeys/42/2025/09/4a0a...-be6a_trip_photo.jpg
 *   ※ CF 오리진 경로가 비어 있다는 전제. 오리진 경로가 '/public'이면 key도 'public/...'로 저장해야 합니다. [4]
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    // ───────────────────────────────────────
    // ① 환경설정 값
    // ───────────────────────────────────────
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;                                      // 업로드 대상 버킷명 (예: heattrip-bucket1)

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudFrontDomain;                            // CF 도메인 (예: https://dxxxxx.cloudfront.net)

    private final AmazonS3 amazonS3;                            // v1 SDK 클라이언트 (Config에서 Bean 주입) [5]

    // ───────────────────────────────────────
    // ② 업로드 정책(검증) — '서비스 레이어'에서 책임지도록 표준화
    // ───────────────────────────────────────
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "image/gif"
    );                                                          // 허용 MIME 타입 화이트리스트 [6]

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024; // 10MB 제한(요구사항에 맞게 조정) [7]

    private static final DateTimeFormatter YYYY = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MM   = DateTimeFormatter.ofPattern("MM");

    // ───────────────────────────────────────
    // ③ 퍼블릭 메서드 — userId 구분 포함 (권장)
    //    DB에는 'URL' 대신 'key'만 저장하는 것을 권장합니다. [8]
    // ───────────────────────────────────────
    public String uploadFile(MultipartFile file, String userId) {
        // 1) 입력 검증 — 크기/타입
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Too large (max 10MB)");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported type: " + file.getContentType());
        }

        // 2) 키 설계 — 접두사(prefix) + UUID + 정규화 파일명 [3], [9]
        LocalDate now = LocalDate.now();
        String yyyy = now.format(YYYY);
        String mm   = now.format(MM);

        String safeName = sanitize(file.getOriginalFilename()); // 특수문자 정리 + 확장자 보존
        String key = String.format(
            "journeys/%s/%s/%s/%s_%s",
            userId, yyyy, mm, UUID.randomUUID(), safeName
        );

        // 3) 메타데이터 — Content-Type + Cache-Control (CDN 캐시 최적화) [10]
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(file.getSize());
        meta.setContentType(file.getContentType());
        // 1년 캐시 + 변하지 않는 리소스(immutable) — 파일명이 UUID로 버전이 바뀌므로 안전하게 롱캐시 가능
        meta.setCacheControl("public, max-age=31536000, immutable");

        // 4) 업로드 — ACL은 사용하지 않는 것이 원칙(버킷 Object Ownership: Bucket owner enforced 권장) [11]
        try (var in = file.getInputStream()) {
            PutObjectRequest req = new PutObjectRequest(bucket, key, in, meta);
            amazonS3.putObject(req);
        } catch (IOException e) {
            throw new RuntimeException("S3 upload failed", e);
        }

        // 5) 반환 — 화면에는 CF URL을 사용(도메인 변경에 대비해 DB에는 key만 저장 권장) [8]
        String base = cloudFrontDomain.endsWith("/")
            ? cloudFrontDomain.substring(0, cloudFrontDomain.length() - 1)
            : cloudFrontDomain;
        return base + "/" + key;
    }

    // (호환용) 기존 시그니처 — userId를 모르면 'anonymous' 같은 접두사를 사용하도록 위임
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, "anonymous");
    }

    // ───────────────────────────────────────
    // ④ 파일명 정규화(sanitize)
    //    - 한글/공백/특수문자 → 안전한 문자로 치환
    //    - 확장자 유지
    // ───────────────────────────────────────
    private String sanitize(String original) {
        if (original == null || original.isBlank()) return "unnamed";
        int dot = original.lastIndexOf('.');
        String base = (dot > 0 ? original.substring(0, dot) : original);
        String ext  = (dot > 0 ? original.substring(dot) : ""); // .jpg, .png ...
        // 영문/숫자/.-_ 만 허용, 나머지 '_'
        base = base.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        // 확장자는 소문자로 통일(브라우저 호환성)
        ext  = ext.toLowerCase();
        return base + ext;
    }
}
