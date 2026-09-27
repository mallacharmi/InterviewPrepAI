package com.interviewprep.controller.api;

import com.interviewprep.dto.request.ProfileUpdateRequest;
import com.interviewprep.dto.response.UserResponse;
import com.interviewprep.security.CustomUserDetails;
import com.interviewprep.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserApiController {

    private final UserService userService;
    private final com.interviewprep.service.FaceVerificationService faceVerificationService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get current user info for id: {}", userDetails.getId());
        return ResponseEntity.ok(userService.getCurrentUser(userDetails));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProfileUpdateRequest request) {
        log.info("REST request to update profile for user: {}", userDetails.getId());
        return ResponseEntity.ok(userService.updateProfile(userDetails.getId(), request));
    }

    @PostMapping("/me/verify-face")
    public ResponseEntity<com.interviewprep.dto.response.FaceVerifyResponse> verifyFace(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody com.interviewprep.dto.request.FaceVerifyRequest request) {
        log.info("REST request to verify face for user: {}", userDetails.getId());
        UserResponse user = userService.getCurrentUser(userDetails);
        com.interviewprep.dto.response.FaceVerifyResponse response = 
                faceVerificationService.verifyFace(user.getProfilePhoto(), request.getLiveImage());
        return ResponseEntity.ok(response);
    }
}
