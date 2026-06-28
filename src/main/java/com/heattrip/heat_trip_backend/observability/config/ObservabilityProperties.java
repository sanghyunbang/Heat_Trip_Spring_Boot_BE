package com.heattrip.heat_trip_backend.observability.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * 관측성(로깅/알림) 설정 바인딩. {@code observability.*} 프로퍼티에 매핑된다.
 *
 * <p>application.properties의 키가 곧 스펙이다:
 * <pre>
 * observability.enabled
 * observability.service-name
 * observability.stacktrace-lines
 * observability.request-log-enabled
 * observability.rate-limit.{enabled,window-seconds}
 * observability.slack.{enabled,webhook-url,channel,username}
 * observability.openai.{enabled}
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "observability")
@Getter
@Setter
public class ObservabilityProperties {

    /** 관측성 기능 전체 on/off */
    private boolean enabled = true;

    /** 알림/로그에 표기할 서비스 이름 */
    private String serviceName = "heat-trip-backend";

    /** 에러 이벤트에 포함할 스택트레이스 라인 수 */
    private int stacktraceLines = 25;

    /** 요청 로깅 Aspect on/off */
    private boolean requestLogEnabled = true;

    private RateLimit rateLimit = new RateLimit();
    private Slack slack = new Slack();
    private Openai openai = new Openai();

    /** 동일 에러 알림 폭주를 막는 레이트 리밋 설정 */
    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
        /** 동일 지문(fingerprint) 알림 억제 윈도우(초) */
        private long windowSeconds = 300;
    }

    /** Slack 알림 설정 */
    @Getter
    @Setter
    public static class Slack {
        private boolean enabled = false;
        private String webhookUrl;
        private String channel = "#backend-alerts";
        private String username = "heat-trip-alert-bot";
    }

    /** 에러 요약용 OpenAI(Spring AI) 설정 */
    @Getter
    @Setter
    public static class Openai {
        private boolean enabled = true;
    }
}
