package com.heattrip.heat_trip_backend.schedules.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.heattrip.heat_trip_backend.OAuth.jwt.JWTProvider;
import com.heattrip.heat_trip_backend.S3.S3Service;
import com.heattrip.heat_trip_backend.schedules.DTO.JourneyRequestDto;
import com.heattrip.heat_trip_backend.schedules.DTO.JourneyResponseDto;
import com.heattrip.heat_trip_backend.schedules.Service.JourneyService;
import com.heattrip.heat_trip_backend.schedules.entity.Journey;
import com.heattrip.heat_trip_backend.user.entity.User;
import com.heattrip.heat_trip_backend.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/journeys")
public class JourneyController {
    private final JourneyService journeyService;
    private final JWTProvider jwtProvider;
    private final UserService userService;
    private final S3Service s3Service;

    public JourneyController(JourneyService journeyService, JWTProvider jwtProvider, UserService userService, S3Service s3Service) {
        this.journeyService = journeyService;
        this.jwtProvider = jwtProvider;
        this.userService = userService;
        this.s3Service = s3Service;
    }

    private User getUserFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;

        String token = authHeader.substring(7);
        if (!jwtProvider.validateToken(token)) return null;

        String userId = jwtProvider.getUserIdFromToken(token);
        return userService.findByEmail(userId);
    }

    @GetMapping("/entries")
    public ResponseEntity<?> getMyJourneys(HttpServletRequest request) {
        User user = getUserFromRequest(request);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        List<Journey> journeys = journeyService.getJourneysByUser(user);
        List<JourneyResponseDto> dtos = journeys.stream()
            .map(JourneyResponseDto::new)
            .toList();

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/entries")
    public ResponseEntity<?> postJourney(@RequestBody JourneyRequestDto dto, HttpServletRequest request) {
        System.out.println("journey post 진입 \n    들어온 값 : " + dto);
        User user = getUserFromRequest(request);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        Journey saved = journeyService.saveJourney(dto, user);
        return ResponseEntity.ok(new JourneyResponseDto(saved));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(HttpServletRequest request) {
        User user = getUserFromRequest(request);
        if (user == null) return ResponseEntity.status(401).body("Unauthorized");

        List<Journey> entries = journeyService.getJourneysByUser(user);
        int count = entries.size();

        Map<String, Object> stats = Map.of("entryCount", count);
        return ResponseEntity.ok(stats);
    }
    //이미지 업로드 관련 메서드
    @PostMapping("/entries/images")
public ResponseEntity<?> uploadDiaryImages(
    @RequestParam("images") List<MultipartFile> images,
    HttpServletRequest request
) {
    User user = getUserFromRequest(request);
    if (user == null) {
        return ResponseEntity.status(401).body("Unauthorized");
    }

    try {
        List<String> uploadedUrls = images.stream()
            .map(s3Service::uploadFile)
            .toList();

        return ResponseEntity.ok(uploadedUrls);
    } catch (Exception e) {
        return ResponseEntity.status(500).body("Image upload failed");
    }
}

}

