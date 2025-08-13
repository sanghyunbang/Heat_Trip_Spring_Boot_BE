package com.heattrip.heat_trip_backend.schedules.controller;

import com.heattrip.heat_trip_backend.schedules.entity.Schedule;
import com.heattrip.heat_trip_backend.OAuth.jwt.JWTProvider;
import com.heattrip.heat_trip_backend.schedules.DTO.ScheduleResponseDto;
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

    // --------------조회(미완성)
    @GetMapping("/schedules")
public ResponseEntity<?> getMySchedules(HttpServletRequest request) {
    System.out.println("                리스트 호출");
    String authHeader = request.getHeader("Authorization");
    System.out.println("                리스트 호출 authHeader : " + authHeader);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(401).body("인증 토큰이 없습니다.");
    }

    String token = authHeader.substring(7);

    if (!jwtProvider.validateToken(token)) {
        return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
    }

    String userId = jwtProvider.getUserIdFromToken(token);
    User user = userService.findByEmail(userId);

    if (user == null) {
        return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
    }

    // ⭐ 유저 기준으로 스케줄 조회
    List<Schedule> schedules = scheduleService.findByUser(user);

    // 필요시 DTO로 변환
    List<ScheduleResponseDto> dtos = schedules.stream()
        .map(ScheduleResponseDto::new)
        .toList();

        System.out.println("        들어가 있는 값 체크 : \n"+dtos);
    return ResponseEntity.ok(dtos);
}

    // --------------등록 
    @PostMapping("/schedules")
    public ResponseEntity<?> createSchedule(@RequestBody Schedule schedule, HttpServletRequest request) {
        System.out.println("       post schedule 진입.");
        String authHeader = request.getHeader("Authorization");
        System.out.println("         authHeader 체크 : "+authHeader);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(401).body("PostMapping schedules : 인증 토큰이 없습니다.");
    }
    String token = authHeader.substring(7); // Bearer 제거

    if (!jwtProvider.validateToken(token)) {
        return ResponseEntity.status(401).body("유효하지 않은 토큰입니다.");
    }
    String userId = jwtProvider.getUserIdFromToken(token);
    User user = userService.findByEmail(userId);
    if (user == null) {
        return ResponseEntity.status(404).body("사용자를 찾을 수 없습니다.");
    }

    // 작성자 정보 주입
    schedule.setUser(user);
        
        Schedule saved = scheduleService.save(schedule);
        return ResponseEntity.ok(saved);
    }

    // --------------유저정보[닉네임 / 본명]
    @GetMapping("/getuser")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    System.out.println("GetUser 진입");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(401).body("getUser clss : 인증 토큰이 없습니다.");
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
