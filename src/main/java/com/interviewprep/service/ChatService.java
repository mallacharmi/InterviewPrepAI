package com.interviewprep.service;

import com.interviewprep.dto.ai.ChatMessageDto;
import com.interviewprep.dto.request.ChatRequest;
import com.interviewprep.dto.response.ChatResponse;
import com.interviewprep.entity.*;
import com.interviewprep.entity.enums.ChatContextType;
import com.interviewprep.exception.ResourceNotFoundException;
import com.interviewprep.exception.UnauthorizedAccessException;
import com.interviewprep.repository.*;
import com.interviewprep.service.ai.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AIService aiService;
    private final AtsService atsService;

    public ChatResponse processChatMessage(Long userId, ChatRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        ChatContextType contextType;
        try {
            contextType = ChatContextType.valueOf(request.getContextType().trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid contextType. Must be PREP, REVIEW, or RESUME");
        }

        Long contextId = request.getContextId();

        // 🔒 Security Check: Enforce session ownership to prevent cross-candidate data leakage
        if (contextType == ChatContextType.REVIEW) {
            if (contextId == null) {
                throw new IllegalArgumentException("contextId (interviewId) is required for REVIEW mode.");
            }
            Interview interview = interviewRepository.findById(contextId)
                    .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", contextId));
            if (!interview.getUser().getId().equals(userId)) {
                log.warn("Security Alert: User {} attempted unauthorized access to session {}", userId, contextId);
                throw new UnauthorizedAccessException("You are strictly unauthorized to query or access another candidate's session data.");
            }
        }

        // Build distinct persona & prompt per mode
        String systemPrompt = buildSystemPrompt(contextType, contextId, request);

        log.info("================================================================================");
        log.info("[CHAT SERVICE REQUEST] User ID: {}, Context Type: {}, Context ID: {}", userId, contextType, contextId);
        log.info("[CHAT SERVICE USER MESSAGE] '{}'", request.getMessage());
        log.info("[CHAT SERVICE ASSEMBLED SYSTEM PROMPT]\n{}", systemPrompt);

        // Fetch recent conversation history from DB (or client fallback)
        List<ChatMessage> dbMessages = contextId != null
                ? chatMessageRepository.findByUserIdAndContextTypeAndContextIdOrderByCreatedAtAsc(userId, contextType, contextId)
                : chatMessageRepository.findByUserIdAndContextTypeAndContextIdNullOrderByCreatedAtAsc(userId, contextType);

        List<ChatMessageDto> historyDtoList = dbMessages.stream()
                .map(m -> new ChatMessageDto(m.getRole(), m.getContent()))
                .collect(Collectors.toList());

        if (historyDtoList.isEmpty() && request.getConversationHistory() != null) {
            historyDtoList = request.getConversationHistory();
        }

        // Save candidate's user message to DB
        ChatMessage userMsg = ChatMessage.builder()
                .user(user)
                .contextType(contextType)
                .contextId(contextId)
                .role("user")
                .content(request.getMessage())
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(userMsg);

        // Call unified AI Service integration with explicit error handling & logging
        String aiReply;
        try {
            aiReply = aiService.generateChatResponse(systemPrompt, request.getMessage(), historyDtoList);
            log.info("[CHAT SERVICE RAW AI RESPONSE]\n{}", aiReply);
            log.info("================================================================================");
        } catch (Exception e) {
            log.error("[CHAT SERVICE ERROR] Exception occurred during AI response generation for mode {}: {}", contextType, e.getMessage(), e);
            throw new RuntimeException("AI service failed to generate chat response: " + e.getMessage(), e);
        }

        // Save AI assistant reply to DB
        ChatMessage aiMsg = ChatMessage.builder()
                .user(user)
                .contextType(contextType)
                .contextId(contextId)
                .role("assistant")
                .content(aiReply)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepository.save(aiMsg);

        return ChatResponse.builder()
                .reply(aiReply)
                .contextType(contextType.name())
                .contextId(contextId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatHistory(Long userId, String contextTypeStr, Long contextId) {
        ChatContextType contextType = ChatContextType.valueOf(contextTypeStr.trim().toUpperCase());

        if (contextType == ChatContextType.REVIEW && contextId != null) {
            Interview interview = interviewRepository.findById(contextId)
                    .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", contextId));
            if (!interview.getUser().getId().equals(userId)) {
                throw new UnauthorizedAccessException("Unauthorized to access session chat history.");
            }
        }

        List<ChatMessage> messages = contextId != null
                ? chatMessageRepository.findByUserIdAndContextTypeAndContextIdOrderByCreatedAtAsc(userId, contextType, contextId)
                : chatMessageRepository.findByUserIdAndContextTypeAndContextIdNullOrderByCreatedAtAsc(userId, contextType);

        return messages.stream()
                .map(m -> new ChatMessageDto(m.getRole(), m.getContent()))
                .collect(Collectors.toList());
    }

    public void clearChatHistory(Long userId, String contextTypeStr, Long contextId) {
        ChatContextType contextType = ChatContextType.valueOf(contextTypeStr.trim().toUpperCase());
        if (contextType == ChatContextType.REVIEW) {
            throw new IllegalArgumentException("Clearing chat history is not supported for REVIEW mode.");
        }
        log.info("[CHAT SERVICE CLEAR HISTORY] User ID: {}, Context Type: {}, Context ID: {}", userId, contextType, contextId);
        if (contextId != null) {
            chatMessageRepository.deleteByUserIdAndContextTypeAndContextId(userId, contextType, contextId);
        } else {
            chatMessageRepository.deleteByUserIdAndContextTypeAndContextIdNull(userId, contextType);
        }
    }

    private String buildSystemPrompt(ChatContextType contextType, Long contextId, ChatRequest request) {
        switch (contextType) {
            case PREP:
                return buildPrepSystemPrompt(request);
            case REVIEW:
                return buildReviewSystemPrompt(contextId, request);
            case RESUME:
                return buildResumeSystemPrompt(request);
            default:
                return "You are an expert technical interview assistant.";
        }
    }

    // MODE 1: PREP Mode Prompt Design
    private String buildPrepSystemPrompt(ChatRequest request) {
        String sessionRole = request.getTargetRole() != null ? request.getTargetRole() : "Software Engineer";
        String diff = request.getDifficulty() != null ? request.getDifficulty() : "MEDIUM";
        String topics = request.getTopics() != null && !request.getTopics().isEmpty()
                ? String.join(", ", request.getTopics())
                : "Core Fundamentals, Problem Solving, System Architecture";

        String userMsg = request.getMessage() != null ? request.getMessage() : "";
        String extractedRole = extractRoleFromUserMessage(userMsg);
        String effectiveRole = extractedRole != null ? extractedRole : sessionRole;

        log.info("[PREP MODE ROLE DETECTED] Session Target Role: '{}' | Extracted User Message Role: '{}' | Effective Role Used: '{}'",
                sessionRole, extractedRole != null ? extractedRole : "None", effectiveRole);

        StringBuilder roleContext = new StringBuilder();
        roleContext.append("Session Configured Role: ").append(sessionRole).append("\n");
        if (extractedRole != null && !extractedRole.equalsIgnoreCase(sessionRole)) {
            roleContext.append("User Explicitly Requested Role in Message: ").append(extractedRole).append("\n");
            roleContext.append("IMPORTANT ROLE INSTRUCTION: The candidate's session was set up for [").append(sessionRole)
                    .append("], but their current message explicitly asks about a [").append(extractedRole)
                    .append("] interview. You MUST acknowledge their session role (").append(sessionRole)
                    .append(") AND provide specific guidance tailored to the requested role (").append(extractedRole)
                    .append("). NEVER silently answer for ").append(sessionRole).append(" without addressing ").append(extractedRole).append("!\n");
        } else {
            roleContext.append("Target Role: ").append(sessionRole).append("\n");
        }

        return "MODE_CONTEXT: PREP_COACH\n" +
                "You are an expert AI Interview Coach helping a candidate prepare BEFORE their assessment.\n" +
                roleContext +
                "Difficulty Level: " + diff + "\n" +
                "Focus Topics: " + topics + "\n\n" +
                "MANDATORY PREP GUIDELINES:\n" +
                "1. CRITICAL CONCEPT ACCURACY: When the user asks a direct factual/conceptual question (e.g., 'what is the difference between X and Y', 'explain Z', 'compare A vs B'), you MUST provide the actual technical definition and explanation with concrete specifics. Do NOT respond with meta-commentary about how the topic is 'frequently assessed' or 'important for interviews' — actually answer the question asked with substantive content.\n" +
                "2. NO TEMPLATED META-RESPONSES: NEVER respond with a numbered list of abstract categories like 'Core Definition', 'Key Differences & Trade-offs', 'Production Application' without filling in the actual specific facts under each heading. Every point you make MUST contain real, specific technical content (exact terms, concrete code/class examples, actual runtime behavior) — not a description of what kind of content should go there.\n" +
                "3. NEW ROLE CONTENT ACCURACY: After acknowledging a role mismatch (e.g. session set to Java Developer, user asks for System Architect or DevOps), you MUST generate interview preparation content that is strictly specific and relevant to the NEWLY MENTIONED role only. Do NOT reuse or default to content associated with the original session role.\n" +
                "4. PAST SCORES: You have NO access to specific past session score data in PREP mode. If asked about past scores, explain clearly that PREP mode is for general coaching, and direct them to open their past session Report Card (REVIEW mode).\n" +
                "5. Respond directly to the candidate's latest message: \"" + userMsg + "\".";
    }

    private String extractRoleFromUserMessage(String msg) {
        if (msg == null || msg.isBlank()) return null;
        String lower = msg.toLowerCase();

        if (lower.contains("data engineer")) return "Data Engineer";
        if (lower.contains("data scientist")) return "Data Scientist";
        if (lower.contains("data analyst")) return "Data Analyst";
        if (lower.contains("frontend") || lower.contains("front end") || lower.contains("react developer")) return "Frontend Developer";
        if (lower.contains("devops") || lower.contains("site reliability") || lower.contains("sre")) return "DevOps Engineer";
        if (lower.contains("full stack") || lower.contains("fullstack")) return "Full Stack Developer";
        if (lower.contains("backend") || lower.contains("back end")) return "Backend Developer";
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("ios")) return "Mobile Developer";
        if (lower.contains("qa") || lower.contains("test engineer") || lower.contains("sdet")) return "QA Engineer";
        if (lower.contains("system architect") || lower.contains("cloud architect")) return "System Architect";

        return null;
    }

    // MODE 2: REVIEW Mode Prompt Design (Session-Scoped & Question-Targeted)
    private String buildReviewSystemPrompt(Long interviewId, ChatRequest request) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview", "id", interviewId));

        List<Question> questions = questionRepository.findByInterviewIdOrderBySequenceNumberAsc(interviewId);
        log.info("[REVIEW MODE ENTITY CONTEXT] Fetched Interview Session ID: {}, Status: {}, Overall Score: {}, Total Questions Count: {}",
                interview.getId(), interview.getStatus(), interview.getOverallScore(), questions.size());

        StringBuilder sessionData = new StringBuilder();

        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int displaySeq = q.getSequenceNumber() > 0 ? q.getSequenceNumber() : (i + 1);
            sessionData.append("\n• Question #").append(displaySeq).append(": ").append(q.getQuestionText())
                    .append(" (Topic: ").append(q.getTopic()).append(")\n");
            Answer ans = answerRepository.findByQuestionId(q.getId()).orElse(null);
            if (ans != null) {
                sessionData.append("  Candidate's Submitted Answer: ").append(ans.getAnswerText()).append("\n")
                        .append("  Original Score: ").append(ans.getScore()).append("/10\n")
                        .append("  Sub-Scores: Technical Accuracy=").append(ans.getTechnicalAccuracy())
                        .append(", Completeness=").append(ans.getCompleteness())
                        .append(", Clarity=").append(ans.getClarity()).append("\n")
                        .append("  Stored AI Feedback: ").append(ans.getFeedback()).append("\n")
                        .append("  Missing Concepts: ").append(ans.getMissingConcepts()).append("\n")
                        .append("  Improvement Suggestion: ").append(ans.getImprovementSuggestion()).append("\n");
            } else {
                sessionData.append("  Candidate's Submitted Answer: [Unanswered / No Answer Record]\n")
                        .append("  Original Score: 0.0/10\n");
            }
        }

        // Extract targeted question number from candidate's user message
        Integer targetQNum = extractQuestionNumber(request.getMessage());
        StringBuilder targetedContext = new StringBuilder();

        if (targetQNum != null) {
            final int reqNum = targetQNum;
            Question targetQ = null;
            if (reqNum >= 1 && reqNum <= questions.size()) {
                targetQ = questions.stream()
                        .filter(q -> q.getSequenceNumber() == reqNum)
                        .findFirst()
                        .orElse(questions.get(reqNum - 1));
            }

            boolean matchFound = (targetQ != null);
            log.info("[REVIEW MODE QUESTION VALIDATION] Requested Question #{}, Session Question Count: {}, Match Found: {}",
                    targetQNum, questions.size(), matchFound);

            if (matchFound) {
                Answer targetAns = answerRepository.findByQuestionId(targetQ.getId()).orElse(null);
                log.info("[REVIEW MODE ENTITY DATA FOUND] Question #{}: Text='{}', Answer='{}', Score={}, Feedback='{}'",
                        targetQNum, targetQ.getQuestionText(),
                        targetAns != null ? targetAns.getAnswerText() : "None",
                        targetAns != null ? targetAns.getScore() : 0.0,
                        targetAns != null ? targetAns.getFeedback() : "None");

                targetedContext.append("\n=== TARGETED QUESTION CONTEXT (QUESTION #").append(targetQNum).append(") ===\n")
                        .append("Question ID: ").append(targetQ.getId()).append("\n")
                        .append("Question Sequence Number: ").append(targetQNum).append("\n")
                        .append("Question Text: ").append(targetQ.getQuestionText()).append("\n")
                        .append("Topic: ").append(targetQ.getTopic()).append("\n")
                        .append("Expected Concepts: ").append(targetQ.getExpectedConcepts()).append("\n")
                        .append("Candidate's Submitted Answer: ").append(targetAns != null && targetAns.getAnswerText() != null ? targetAns.getAnswerText() : "[Unanswered / No Answer Record]").append("\n")
                        .append("Overall Score: ").append(targetAns != null && targetAns.getScore() != null ? targetAns.getScore() : 0.0).append("/10\n")
                        .append("Technical Accuracy: ").append(targetAns != null && targetAns.getTechnicalAccuracy() != null ? targetAns.getTechnicalAccuracy() : 0.0).append("\n")
                        .append("Completeness: ").append(targetAns != null && targetAns.getCompleteness() != null ? targetAns.getCompleteness() : 0.0).append("\n")
                        .append("Clarity: ").append(targetAns != null && targetAns.getClarity() != null ? targetAns.getClarity() : 0.0).append("\n")
                        .append("Stored AI Feedback: ").append(targetAns != null && targetAns.getFeedback() != null ? targetAns.getFeedback() : "None").append("\n")
                        .append("Missing Concepts: ").append(targetAns != null && targetAns.getMissingConcepts() != null ? targetAns.getMissingConcepts() : "None").append("\n")
                        .append("Improvement Suggestion: ").append(targetAns != null && targetAns.getImprovementSuggestion() != null ? targetAns.getImprovementSuggestion() : "None").append("\n")
                        .append("==========================================================\n");
            } else {
                log.warn("[REVIEW MODE QUESTION OUT OF RANGE] Question #{} requested by candidate, but session {} only contains {} total questions.",
                        targetQNum, interviewId, questions.size());
                targetedContext.append("\n=== TARGETED QUESTION NOTICE ===\n")
                        .append("NOTICE: The candidate asked about Question #").append(targetQNum)
                        .append(", but this session ONLY has ").append(questions.size())
                        .append(" total questions (Question #").append(targetQNum).append(" DOES NOT EXIST in this session).\n")
                        .append("You MUST explicitly inform the candidate that Question #").append(targetQNum)
                        .append(" does not exist in this session and state that this session only has ").append(questions.size()).append(" total questions (Questions 1 to ").append(questions.size()).append(").\n")
                        .append("DO NOT return data for a different question as if it were Question #").append(targetQNum).append("!\n")
                        .append("================================\n");
            }
        } else {
            log.info("[REVIEW MODE ENTITY CONTEXT] No specific question number requested in user message.");
        }

        return "MODE_CONTEXT: SESSION_REVIEW\n" +
                "You are a Performance Review Tutor analyzing a completed interview session.\n" +
                "SESSION METADATA:\n" +
                "Session ID: " + interview.getId() + "\n" +
                "Target Role: " + interview.getTargetRole() + " (" + interview.getInterviewType() + ")\n" +
                "Overall Session Score: " + interview.getOverallScore() + "/10\n" +
                "Status: " + interview.getStatus() + "\n\n" +
                "ALL SESSION QUESTIONS & ANSWERS:\n" + sessionData + "\n" +
                targetedContext + "\n" +
                "MANDATORY GUIDELINES:\n" +
                "1. You MUST reference the specific question text, the candidate's actual submitted answer, and the stored score/feedback in your response.\n" +
                "2. Do NOT give generic advice — explain specifically what was missing or incorrect in THIS candidate's answer for Question #" + (targetQNum != null ? targetQNum : "referenced") + ".\n" +
                "3. If the requested question number does not exist in this session, explicitly notify the candidate that Question #" + (targetQNum != null ? targetQNum : "N") + " was not found (session has " + questions.size() + " total questions).\n" +
                "4. Restrict all explanations strictly to this candidate's own assessment session data.";
    }

    private Integer extractQuestionNumber(String text) {
        if (text == null || text.isBlank()) return null;
        String lower = text.toLowerCase().trim();

        // Pattern 1: "question 3", "q3", "question #3", "q. 3", "question no 3"
        java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("(?:question|q|item|number|no\\.?)\\s*#?\\s*(\\d+)").matcher(lower);
        if (m1.find()) {
            try {
                return Integer.parseInt(m1.group(1));
            } catch (NumberFormatException ignored) {}
        }

        // Pattern 2: "3rd question", "1st question", "2nd question", "4th question"
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("(\\d+)(?:st|nd|rd|th)\\s+(?:question|q)").matcher(lower);
        if (m2.find()) {
            try {
                return Integer.parseInt(m2.group(1));
            } catch (NumberFormatException ignored) {}
        }

        // Pattern 3: word numbers "question one", "question two", etc.
        String[] wordNums = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
        for (int i = 0; i < wordNums.length; i++) {
            if (lower.contains("question " + wordNums[i]) || lower.contains("q " + wordNums[i])) {
                return i + 1;
            }
        }

        return null;
    }

    // MODE 3: RESUME Mode Prompt Design
    private String buildResumeSystemPrompt(ChatRequest request) {
        String resume = request.getResumeText() != null ? request.getResumeText() : "";
        String jd = request.getJobDescription() != null ? request.getJobDescription() : "";

        List<String> missingKeywordsList = new ArrayList<>();
        Double score = request.getAtsScore();

        if (request.getMissingKeywords() != null && !request.getMissingKeywords().isEmpty()) {
            missingKeywordsList.addAll(request.getMissingKeywords());
        }

        // Server-side fallback: if missing keywords list was not provided in request but resume & JD text exist, run ATS analysis
        if (missingKeywordsList.isEmpty() && !resume.isBlank() && !jd.isBlank()) {
            try {
                com.interviewprep.dto.request.AtsAnalysisRequest atsReq = new com.interviewprep.dto.request.AtsAnalysisRequest(resume, jd);
                com.interviewprep.dto.response.AtsAnalysisResponse atsRes = atsService.analyzeResume(atsReq);
                if (atsRes != null && atsRes.getMissingSkills() != null) {
                    missingKeywordsList.addAll(atsRes.getMissingSkills());
                }
                if (score == null && atsRes != null) {
                    score = atsRes.getAtsScore();
                }
            } catch (Exception e) {
                log.warn("[RESUME MODE ATS LOOKUP WARN] Server-side ATS analysis lookup exception: {}", e.getMessage());
            }
        }

        String missingStr = !missingKeywordsList.isEmpty()
                ? String.join(", ", missingKeywordsList)
                : "None identified";

        log.info("[RESUME MODE FETCHED MISSING KEYWORDS] Missing keywords fetched for resume context: {}", missingKeywordsList);
        log.info("[RESUME MODE ENTITY DATA] Resume snippet length: {}, JD snippet length: {}, ATS Score: {}%, Missing Keywords: {}",
                resume.length(), jd.length(), score != null ? score : 0.0, missingStr);

        return "MODE_CONTEXT: RESUME_ATS\n" +
                "You are an ATS Resume Optimization Advisor.\n" +
                "ATS Match Score: " + (score != null ? score : 0.0) + "%\n" +
                "Missing Keywords: " + missingStr + "\n\n" +
                "CONTEXT:\n" +
                "Resume Snippet: " + (resume.length() > 500 ? resume.substring(0, 500) + "..." : (resume.isBlank() ? "[Resume text uploaded]" : resume)) + "\n" +
                "Job Description Snippet: " + (jd.length() > 500 ? jd.substring(0, 500) + "..." : (jd.isBlank() ? "[Job description provided]" : jd)) + "\n\n" +
                "GUIDELINES:\n" +
                "1. Provide concrete, actionable bullet point rewording suggestions for this specific candidate resume.\n" +
                "2. Show BEFORE and AFTER bullet points integrating missing keywords (" + missingStr + ").\n" +
                "3. Emphasize quantifiable metrics (e.g. latency improvement, scalability, system throughput).";
    }
}
