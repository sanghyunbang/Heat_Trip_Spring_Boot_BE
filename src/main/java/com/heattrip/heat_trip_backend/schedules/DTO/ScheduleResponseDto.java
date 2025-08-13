package com.heattrip.heat_trip_backend.schedules.DTO;

import java.time.LocalDate;
import com.heattrip.heat_trip_backend.schedules.entity.Schedule;

import lombok.Data;

@Data
public class ScheduleResponseDto {
    private Integer scheduleId;
    private String title;
    private String content;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String userNickname;

    public ScheduleResponseDto(Schedule schedule) {
        this.scheduleId = schedule.getScheduleId();
        this.title = schedule.getTitle();
        this.content = schedule.getContent();
        this.dateFrom = schedule.getDateFrom();
        this.dateTo = schedule.getDateTo();
        this.userNickname = schedule.getUser().getNickname(); // 예: user 닉네임
    }

    // Getters 생략 가능 (Lombok 써도 OK)
}

