package com.heattrip.heat_trip_backend.schedules.DTO;

import java.time.LocalDate;
import java.util.List;

import com.heattrip.heat_trip_backend.schedules.entity.Journey;

import lombok.Data;

@Data
public class JourneyResponseDto {
    private Integer id;
    private String title;
    private LocalDate date;
    private String location;
    private String weatherLabel;
    private String moodLabel;
    private String body;
    private List<String> photos;
    private String userNickname;

    public JourneyResponseDto(Journey journey) {
        this.id = journey.getId();
        this.title = journey.getTitle();
        this.date = journey.getDate();
        this.location = journey.getLocation();
        this.weatherLabel = journey.getWeatherLabel();
        this.moodLabel = journey.getMoodLabel();
        this.body = journey.getBody();
        this.photos = journey.getPhotos();
        this.userNickname = journey.getUser().getNickname();
    }
}
