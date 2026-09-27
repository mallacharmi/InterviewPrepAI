package com.interviewprep.controller.api;

import com.interviewprep.dto.request.AtsAnalysisRequest;
import com.interviewprep.dto.request.AtsCreateInterviewRequest;
import com.interviewprep.dto.request.InterviewCreateRequest;
import com.interviewprep.dto.response.AtsAnalysisResponse;
import com.interviewprep.dto.response.InterviewResponse;
import com.interviewprep.security.CustomUserDetails;
import com.interviewprep.service.AtsService;
import com.interviewprep.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/ats")
@RequiredArgsConstructor
@Slf4j
public class AtsApiController {

    private final AtsService atsService;
    private final InterviewService interviewService;

    @PostMapping("/analyze")
    public ResponseEntity<AtsAnalysisResponse> analyzeAts(@Valid @RequestBody AtsAnalysisRequest request) {
        log.info("REST request to analyze ATS Resume and Job Description");
        return ResponseEntity.ok(atsService.analyzeResume(request));
    }

    @PostMapping("/create-interview")
    public ResponseEntity<InterviewResponse> createInterviewFromAts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AtsCreateInterviewRequest request) {
        log.info("REST request to create practice interview from ATS JD for user: {}", userDetails.getId());

        Set<String> topicsSet = new LinkedHashSet<>();
        if (request.getMatchedKeywords() != null) {
            topicsSet.addAll(request.getMatchedKeywords());
        }
        if (request.getMissingSkills() != null) {
            topicsSet.addAll(request.getMissingSkills());
        }
        if (topicsSet.isEmpty()) {
            topicsSet.addAll(List.of("Java", "Spring Boot", "System Design", "SQL"));
        }

        String role = request.getTargetRole();
        if (role == null || role.isBlank()) {
            role = "Java Developer";
        }

        InterviewCreateRequest createReq = InterviewCreateRequest.builder()
                .targetRole(role)
                .interviewType("TECHNICAL")
                .difficulty("MEDIUM")
                .totalQuestions(5)
                .topics(new ArrayList<>(topicsSet))
                .build();

        InterviewResponse response = interviewService.createInterview(userDetails.getId(), createReq);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/upload-and-analyze")
    public ResponseEntity<AtsAnalysisResponse> uploadAndAnalyze(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("jobDescription") String jobDescription) {
        log.info("REST request to upload and analyze ATS resume file: {}", file.getOriginalFilename());
        String extractedText = atsService.extractTextFromFile(file);
        AtsAnalysisRequest request = new AtsAnalysisRequest();
        request.setResumeText(extractedText);
        request.setJobDescription(jobDescription);
        return ResponseEntity.ok(atsService.analyzeResume(request));
    }

    @PostMapping("/extract-resume-text")
    public ResponseEntity<java.util.Map<String, String>> extractResumeText(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        log.info("REST request to extract text from resume file: {}", file.getOriginalFilename());
        String extractedText = atsService.extractTextFromFile(file);
        return ResponseEntity.ok(java.util.Map.of(
                "text", extractedText,
                "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        ));
    }
}
