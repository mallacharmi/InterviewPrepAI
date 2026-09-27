package com.interviewprep.controller.api;

import com.interviewprep.dto.response.DashboardResponse;
import com.interviewprep.security.CustomUserDetails;
import com.interviewprep.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardApiController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get dashboard metrics for user: {}", userDetails.getId());
        return ResponseEntity.ok(dashboardService.getDashboard(userDetails.getId()));
    }
}
