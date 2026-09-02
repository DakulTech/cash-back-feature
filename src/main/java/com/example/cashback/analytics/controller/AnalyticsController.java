package com.example.cashback.analytics.controller;

import com.example.cashback.analytics.dto.UserAnalyticsDTO;
import com.example.cashback.analytics.service.UserAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final UserAnalyticsService userAnalyticsService;

    @GetMapping("/overview/{userId}")
    public ResponseEntity<UserAnalyticsDTO> getUserAnalytics(@PathVariable UUID userId) {
        return ResponseEntity.ok(userAnalyticsService.getUserAnalytics(userId));
    }
}
