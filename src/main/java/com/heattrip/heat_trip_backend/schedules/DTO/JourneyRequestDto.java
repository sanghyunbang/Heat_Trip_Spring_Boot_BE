package com.heattrip.heat_trip_backend.schedules.DTO;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class JourneyRequestDto {
    private Integer scheduleId;
    private String title;
    private LocalDate date;
    private String location;
    private String weatherLabel;
    private String moodLabel;
    private String body;
    private List<String> photos;
}

