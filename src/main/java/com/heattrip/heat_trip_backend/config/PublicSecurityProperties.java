package com.heattrip.heat_trip_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 공개 운영용 보안 설정 바인딩. {@code app.security.*} 프로퍼티에 매핑된다.
 *
 * <p>application.properties의 키가 곧 스펙이다:
 * <pre>
 * app.security.docs-public
 * app.security.allowed-origins            (콤마 구분 문자열)
 * app.security.rate-limit.enabled
 * app.security.rate-limit.ip-header
 * app.security.rate-limit.window-seconds
 * app.security.rate-limit.{login,curation,upload,feedback,search}-max-requests
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class PublicSecurityProperties {

    /** Swagger/OpenAPI 문서를 공개할지 여부 */
    private boolean docsPublic = false;

    /** CORS 허용 오리진(콤마 구분) */
    private String allowedOrigins = "http://localhost:8080,http://10.0.2.2:8080";

    private RateLimit rateLimit = new RateLimit();

    /** 엔드포인트 버킷별 API 레이트 리밋 설정 */
    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;

        /** 클라이언트 IP를 읽을 헤더 (프록시 뒤일 때) */
        private String ipHeader = "X-Forwarded-For";

        /** 고정 윈도우 길이(초) */
        private long windowSeconds = 60;

        private int loginMaxRequests = 10;
        private int curationMaxRequests = 30;
        private int uploadMaxRequests = 20;
        private int feedbackMaxRequests = 30;
        private int searchMaxRequests = 120;
    }
}
