package com.heattrip.heat_trip_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient tourApiWebClient(WebClient.Builder builder) {
        // ExchangeStrategies를 사용하여 버퍼 크기 제한을 늘립니다.
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(clientCodecConfigurer -> clientCodecConfigurer
                    .defaultCodecs()
                    .maxInMemorySize(10 * 1024 * 1024) // 예시: 10MB로 설정 (10 * 1024 * 1024 bytes)
                )
                .build();
        return builder
                .baseUrl("https://apis.data.go.kr/B551011/KorService2")
                .exchangeStrategies(exchangeStrategies)
                .build();
    }
}
