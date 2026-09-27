package com.interviewprep.controller.api;

import com.interviewprep.dto.request.AnswerSubmitRequest;
import com.interviewprep.dto.request.InterviewCreateRequest;
import com.interviewprep.dto.response.EvaluationResponse;
import com.interviewprep.dto.response.InterviewHistoryResponse;
import com.interviewprep.dto.response.InterviewResponse;
import com.interviewprep.dto.response.QuestionResponse;
import com.interviewprep.dto.response.ReportResponse;
import com.interviewprep.security.CustomUserDetails;
import com.interviewprep.service.AnswerService;
import com.interviewprep.service.InterviewService;
import com.interviewprep.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
@Slf4j
public class InterviewApiController {

    private final InterviewService interviewService;
    private final AnswerService answerService;
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<InterviewResponse> createInterview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody InterviewCreateRequest request) {
        log.info("REST request to create interview for user: {}", userDetails.getId());
        InterviewResponse response = interviewService.createInterview(userDetails.getId(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<InterviewHistoryResponse> getInterviewHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        log.info("REST request to get interview history for user: {}", userDetails.getId());
        return ResponseEntity.ok(interviewService.getInterviewHistory(userDetails.getId(), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> getInterview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(interviewService.getInterview(userDetails.getId(), id));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<QuestionResponse> startInterview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(interviewService.startInterview(userDetails.getId(), id));
    }

    @GetMapping("/{id}/question")
    public ResponseEntity<QuestionResponse> getCurrentQuestion(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(interviewService.getCurrentQuestion(userDetails.getId(), id));
    }

    @PostMapping("/{id}/questions/{questionId}/answer")
    public ResponseEntity<EvaluationResponse> submitAnswer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @PathVariable("questionId") Long questionId,
            @Valid @RequestBody AnswerSubmitRequest request) {
        return ResponseEntity.ok(answerService.submitAnswer(userDetails.getId(), id, questionId, request));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ReportResponse> completeInterview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        interviewService.completeInterview(userDetails.getId(), id);
        return ResponseEntity.ok(reportService.generateReport(userDetails.getId(), id));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<ReportResponse> terminateInterview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestBody(required = false) com.interviewprep.dto.request.InterviewTerminateRequest request) {
        String reason = request != null && request.getReason() != null ? request.getReason() : "Proctoring violation limit reached";
        Integer violations = request != null && request.getViolationCount() != null ? request.getViolationCount() : 3;
        interviewService.terminateInterview(userDetails.getId(), id, reason, violations);
        return ResponseEntity.ok(reportService.generateReport(userDetails.getId(), id));
    }

    @PostMapping("/{id}/proctoring-alerts")
    public ResponseEntity<java.util.Map<String, String>> updateProctoringAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestBody com.interviewprep.dto.request.ProctoringAlertsRequest request) {
        interviewService.updateProctoringAlerts(userDetails.getId(), id, request);
        return ResponseEntity.ok(java.util.Map.of("status", "success"));
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<ReportResponse> getReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(reportService.generateReport(userDetails.getId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<java.util.Map<String, Object>> deleteInterview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        interviewService.deleteInterview(userDetails.getId(), id);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Interview deleted successfully"));
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<java.util.Map<String, Object>> batchDeleteInterviews(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody com.interviewprep.dto.request.InterviewBatchDeleteRequest request) {
        int deleted = interviewService.deleteInterviews(userDetails.getId(), request != null ? request.getInterviewIds() : java.util.Collections.emptyList());
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", deleted + " interview(s) deleted successfully", "deletedCount", deleted));
    }

    @PostMapping("/history/clear")
    public ResponseEntity<java.util.Map<String, String>> clearHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        interviewService.clearUserHistory(userDetails.getId());
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Interview history cleared successfully"));
    }

    @PostMapping(value = "/{id}/recording", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<java.util.Map<String, String>> uploadRecording(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String videoUrl = interviewService.saveInterviewRecording(userDetails.getId(), id, file);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "videoUrl", videoUrl));
    }

    @GetMapping("/{id}/video")
    public ResponseEntity<org.springframework.core.io.Resource> getRecordingVideo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id) {
        org.springframework.core.io.Resource resource = interviewService.getInterviewRecordingResource(userDetails.getId(), id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"interview_" + id + ".webm\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("video/webm"))
                .body(resource);
    }

    @PostMapping("/{id}/expressions")
    public ResponseEntity<java.util.Map<String, String>> saveFacialExpressions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestBody com.interviewprep.dto.request.FacialExpressionRequest request) {
        interviewService.saveFacialExpressions(userDetails.getId(), id, request);
        return ResponseEntity.ok(java.util.Map.of("status", "success", "message", "Facial expressions saved successfully"));
    }
}
