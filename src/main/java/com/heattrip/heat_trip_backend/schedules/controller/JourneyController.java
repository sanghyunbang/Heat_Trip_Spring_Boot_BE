package com.heattrip.heat_trip_backend.schedules.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.heattrip.heat_trip_backend.OAuth.jwt.JWTProvider;
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

    public JourneyController(JourneyService journeyService, JWTProvider jwtProvider, UserService userService) {
        this.journeyService = journeyService;
        this.jwtProvider = jwtProvider;
        this.userService = userService;
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
}

