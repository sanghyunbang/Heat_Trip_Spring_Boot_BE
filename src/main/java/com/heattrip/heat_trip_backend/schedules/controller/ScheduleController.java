package com.heattrip.heat_trip_backend.schedules.controller;

import com.heattrip.heat_trip_backend.auth.CurrentUser;
import com.heattrip.heat_trip_backend.schedules.entity.Schedule;
import com.heattrip.heat_trip_backend.schedules.DTO.ScheduleResponseDto;
import com.heattrip.heat_trip_backend.schedules.Service.ScheduleService;
import com.heattrip.heat_trip_backend.user.entity.*;

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

    // -------------- 조회
    @GetMapping("/schedules")
    public ResponseEntity<?> getMySchedules(@CurrentUser User user) {
        List<Schedule> schedules = scheduleService.findByUser(user);

        List<ScheduleResponseDto> dtos = schedules.stream()
                .map(schedule -> {
                    int journeyCount = scheduleService.countJourneysBySchedule(schedule.getScheduleId());
                    return new ScheduleResponseDto(schedule, journeyCount);
                })
                .toList();

        // 빈 결과 처리 (프론트는 204 지원)
        if (dtos.isEmpty()) {
            return ResponseEntity.noContent().build(); // 204
        }

        return ResponseEntity.ok(dtos);
    }

    // -------------- 등록
    @PostMapping("/schedules")
    public ResponseEntity<?> createSchedule(@RequestBody Schedule schedule, @CurrentUser User user) {
        // 작성자 정보 주입
        schedule.setUser(user);

        Schedule saved = scheduleService.save(schedule);
        return ResponseEntity.ok(saved);
    }

    // -------------- 수정
    @PutMapping("/schedules/{scheduleId}")
    public ResponseEntity<?> updateSchedule(
            @PathVariable("scheduleId") Integer scheduleId,
            @RequestBody Schedule updatedSchedule,
            @CurrentUser User user) {

        // 기존 스케줄 조회
        Schedule existing = scheduleService.findById(scheduleId);
        if (existing == null) {
            return ResponseEntity.status(404).body("수정할 스케줄을 찾을 수 없습니다.");
        }

        // 본인 소유 스케줄인지 확인
        if (!existing.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("본인의 스케줄만 수정할 수 있습니다.");
        }

        // 수정 필드 반영
        existing.setTitle(updatedSchedule.getTitle());
        existing.setContent(updatedSchedule.getContent());
        existing.setDateFrom(updatedSchedule.getDateFrom());
        existing.setDateTo(updatedSchedule.getDateTo());

        // 저장
        Schedule saved = scheduleService.save(existing);
        return ResponseEntity.ok(saved);
    }

    // -------------- 삭제
    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<?> deleteSchedule(
            @PathVariable("scheduleId") Integer scheduleId,
            @CurrentUser User user) {

        Schedule existing = scheduleService.findById(scheduleId);
        if (existing == null) {
            return ResponseEntity.status(404).body("삭제할 스케줄을 찾을 수 없습니다.");
        }

        // 본인 소유 스케줄인지 확인
        if (!existing.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("본인의 스케줄만 삭제할 수 있습니다.");
        }

        scheduleService.delete(existing);

        return ResponseEntity.noContent().build();
    }
}
