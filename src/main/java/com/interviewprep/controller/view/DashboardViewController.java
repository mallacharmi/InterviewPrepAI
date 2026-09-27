package com.interviewprep.controller.view;

import com.interviewprep.dto.response.DashboardResponse;
import com.interviewprep.dto.response.UserResponse;
import com.interviewprep.security.CustomUserDetails;
import com.interviewprep.service.DashboardService;
import com.interviewprep.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardViewController {

    private final DashboardService dashboardService;
    private final UserService userService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        DashboardResponse dashboard = dashboardService.getDashboard(userDetails.getId());
        UserResponse user = userService.getUserById(userDetails.getId());
        
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("stats", dashboard);
        model.addAttribute("recentInterviews", dashboard != null ? dashboard.getRecentInterviews() : null);
        model.addAttribute("topicPerformance", dashboard != null ? dashboard.getTopicPerformance() : null);
        model.addAttribute("user", user);
        
        return "dashboard";
    }
}
