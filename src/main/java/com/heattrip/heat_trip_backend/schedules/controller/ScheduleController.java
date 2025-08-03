package com.heattrip.heat_trip_backend.schedules.controller;

import com.heattrip.heat_trip_backend.schedules.entity.Schedule;
import com.heattrip.heat_trip_backend.schedules.Service.ScheduleService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    //조회(미완성)
    @GetMapping("/schedules") 
    public ResponseEntity<?> getAllSchedules() {
        List<Schedule> schedules = scheduleService.findAll();
        return ResponseEntity.ok(schedules);
    }

    //등록 (미완성)
    @PostMapping("/schedules")
public ResponseEntity<?> createSchedule(@RequestBody Schedule schedule) {
    Schedule saved = scheduleService.save(schedule);
    return ResponseEntity.ok(saved);
}

}
