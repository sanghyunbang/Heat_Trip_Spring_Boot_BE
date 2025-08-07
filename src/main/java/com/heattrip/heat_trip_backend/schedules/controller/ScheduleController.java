package com.heattrip.heat_trip_backend.schedules.controller;

import com.heattrip.heat_trip_backend.schedules.entity.Schedule;
import com.heattrip.heat_trip_backend.OAuth.jwt.JWTProvider;
import com.heattrip.heat_trip_backend.schedules.Service.ScheduleService;
import com.heattrip.heat_trip_backend.user.entity.*;
import com.heattrip.heat_trip_backend.user.service.*;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final UserService userService;
    private final JWTProvider jwtProvider;
    
    public ScheduleController(ScheduleService scheduleService, UserService userService, JWTProvider jwtProvider) {
        this.scheduleService = scheduleService;
        this.userService = userService;
        this.jwtProvider = jwtProvider;
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

    @GetMapping("/getuser")
public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(401).body("인증 토큰이 없습니다.");
    }

    String token = authHeader.substring(7); // "Bearer " 제거

    if (!jwtProvider.validateToken(token)) {
        return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
    }

    String userId = jwtProvider.getUserIdFromToken(token); // subject에 저장된 값 (예: email 또는 userId)

    // userService를 통해 사용자 조회
    User user = userService.findByEmail(userId); // or findById(userId) depending on what subject stores

    if (user == null) {
        return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
    }

    return ResponseEntity.ok(user);
}


}
