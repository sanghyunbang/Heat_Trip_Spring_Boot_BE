package com.heattrip.heat_trip_backend.tour.schedules;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.heattrip.heat_trip_backend.tour.service.PlaceImportService;
import com.heattrip.heat_trip_backend.tour.service.TourApiClient;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class TourFetchScheduler {
    private final PlaceImportService importer;

    /* 앱 시작시 즉시 1회 실행 */
    // @PostConstruct
    // public void runOnStartUp() throws Exception {
    //     importer.fullImportAllAreas();
    // }

    /* 매일 02:10에 실행 */
    @Scheduled(cron = "0 10 2 * * *", zone = "Asia/Seoul")
    public void runDaily() throws Exception {
        importer.fullImportAllAreas();
    }
}
