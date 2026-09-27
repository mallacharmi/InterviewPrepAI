package com.interviewprep.controller.view;

import com.interviewprep.dto.request.RegisterRequest;
import com.interviewprep.exception.UserAlreadyExistsException;
import com.interviewprep.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthViewController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            log.warn("Registration binding errors: {}", bindingResult.getAllErrors());
            model.addAttribute("errorMessage", "Please fix the validation errors below.");
            return "register";
        }
        
        try {
            userService.registerUser(request);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (UserAlreadyExistsException e) {
            model.addAttribute("errorMessage", "An account with this email address already exists! Please log in instead.");
            return "register";
        } catch (Exception e) {
            log.error("Error during registration", e);
            model.addAttribute("errorMessage", "An error occurred during registration. Please try again.");
            return "register";
        }
    }
}
