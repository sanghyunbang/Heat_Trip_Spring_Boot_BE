package com.heattrip.heat_trip_backend.tour.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourApiClient {
    private final WebClient tourApiWebClient; // WebClientConfig에서 @Bean 등록 된 것을 주입
        
    @Value("${TOUR.API.SECRET}")
    private String secret;

    public String fetchAreaBasedList(int areaCode, int pageNo, int numOfRows) {
        return tourApiWebClient.get()
            .uri(uri -> uri.path("/areaBasedList2")
                .queryParam("serviceKey", secret)
                .queryParam("MobileOS", "WEB")
                .queryParam("MobileApp", "HeatTrip")
                .queryParam("arrange", "C")
                .queryParam("areaCode", areaCode)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", numOfRows)
                .queryParam("_type", "json")          // ← JSON으로 받기
                .build()
            )
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
}
