package com.interviewprep.controller.view;

import com.interviewprep.dto.response.InterviewResponse;
import com.interviewprep.dto.response.UserResponse;
import com.interviewprep.security.CustomUserDetails;
import com.interviewprep.service.InterviewService;
import com.interviewprep.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/interviews")
@RequiredArgsConstructor
@Slf4j
public class InterviewViewController {

    private final UserService userService;
    private final InterviewService interviewService;
    private final com.interviewprep.service.ReportService reportService;

    @GetMapping({"/setup", "/new"})
    public String setupInterview(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        UserResponse user = userService.getUserById(userDetails.getId());
        model.addAttribute("user", user);
        return "interview-setup";
    }

    @GetMapping("/history")
    public String interviewHistory(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        UserResponse user = userService.getUserById(userDetails.getId());
        model.addAttribute("user", user);
        model.addAttribute("interviews", interviewService.getUserInterviews(userDetails.getId()));
        return "history";
    }

    @GetMapping("/{id:[0-9]+}")
    public String liveInterview(@PathVariable("id") Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        // 🔒 SECURITY OWNERSHIP CHECK: Verify logged-in candidate owns interview session {id}
        InterviewResponse interview = interviewService.getInterview(userDetails.getId(), id);

        UserResponse user = userService.getUserById(userDetails.getId());
        model.addAttribute("user", user);
        model.addAttribute("interviewId", id);
        model.addAttribute("interview", interview);
        model.addAttribute("currentQuestionNumber", interview.getCompletedQuestions() + 1);

        try {
            com.interviewprep.dto.response.QuestionResponse currentQ = interviewService.getCurrentQuestion(userDetails.getId(), id);
            model.addAttribute("currentQuestion", currentQ);
        } catch (Exception ex) {
            log.info("No active unanswered question for interview {}: {}", id, ex.getMessage());
        }

        return "interview";
    }

    @GetMapping("/{id:[0-9]+}/report")
    public String interviewReport(@PathVariable("id") Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        // 🔒 SECURITY OWNERSHIP CHECK: Verify logged-in candidate owns interview report card {id}
        InterviewResponse interview = interviewService.getInterview(userDetails.getId(), id);
        com.interviewprep.dto.response.ReportResponse report = reportService.generateReport(userDetails.getId(), id);

        UserResponse user = userService.getUserById(userDetails.getId());
        model.addAttribute("user", user);
        model.addAttribute("interviewId", id);
        model.addAttribute("interview", interview);
        model.addAttribute("report", report);

        return "report";
    }

    @GetMapping("/{id:[0-9]+}/detail")
    public String interviewDetail(@PathVariable("id") Long id, @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        // 🔒 SECURITY OWNERSHIP CHECK: Verify logged-in candidate owns interview detail {id}
        InterviewResponse interview = interviewService.getInterview(userDetails.getId(), id);

        UserResponse user = userService.getUserById(userDetails.getId());
        model.addAttribute("user", user);
        model.addAttribute("interviewId", id);
        model.addAttribute("interview", interview);

        return "interview-detail";
    }
}
