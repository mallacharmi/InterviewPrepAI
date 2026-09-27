package com.interviewprep.controller.view;

import com.interviewprep.dto.response.UserResponse;
import com.interviewprep.security.CustomUserDetails;
import com.interviewprep.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AtsViewController {

    private final UserService userService;

    @GetMapping("/ats")
    public String atsAnalyzer(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        UserResponse user = userService.getUserById(userDetails.getId());
        model.addAttribute("user", user);
        return "ats-analyzer";
    }
}
