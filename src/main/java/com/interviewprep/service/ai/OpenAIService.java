package com.interviewprep.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.config.AIConfig;
import com.interviewprep.dto.ai.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
public class OpenAIService implements AIService {

    private final AIConfig aiConfig;
    private final PromptBuilder promptBuilder;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAIService(AIConfig aiConfig, PromptBuilder promptBuilder, RestTemplate restTemplate) {
        this.aiConfig = aiConfig;
        this.promptBuilder = promptBuilder;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean isAvailable() {
        return aiConfig.getApiKey() != null && !aiConfig.getApiKey().isBlank();
    }

    @Override
    public AIQuestionResponse generateQuestion(AIQuestionRequest request) {
        String prompt = promptBuilder.buildQuestionGenerationPrompt(request);
        String json = callOpenAI("You are an expert technical interviewer.", prompt);
        AIQuestionResponse response = parseResponse(json, AIQuestionResponse.class);
        if (response.getQuestion() == null || response.getQuestion().isBlank()) {
            throw new RuntimeException("Generated question is empty");
        }
        return response;
    }

    @Override
    public AIEvaluationResponse evaluateAnswer(AIEvaluationRequest request) {
        String prompt = promptBuilder.buildAnswerEvaluationPrompt(request);
        String json = callOpenAI("You are an expert interview evaluator.", prompt);
        AIEvaluationResponse response = parseResponse(json, AIEvaluationResponse.class);
        if (response.getScore() < 0 || response.getScore() > 10) {
            throw new RuntimeException("Invalid score generated");
        }
        return response;
    }

    @Override
    public AIFollowUpResponse generateFollowUp(AIFollowUpRequest request) {
        String prompt = promptBuilder.buildFollowUpPrompt(request);
        String json = callOpenAI("Generate a follow-up interview question.", prompt);
        return parseResponse(json, AIFollowUpResponse.class);
    }

    @Override
    public String generateChatResponse(String systemPrompt, String userMessage, List<ChatMessageDto> history) {
        log.info("[OPENAI SERVICE] Generating chat response. System Prompt length: {}, User Message: '{}', History turns: {}",
                systemPrompt != null ? systemPrompt.length() : 0, userMessage, history != null ? history.size() : 0);
        java.util.List<Map<String, Object>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt != null ? systemPrompt : "You are an AI assistant."));

        if (history != null && !history.isEmpty()) {
            // Trim to last 10 turns max to control token cost
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                ChatMessageDto msg = history.get(i);
                if (msg != null && msg.getContent() != null && !msg.getContent().isBlank()) {
                    String role = "user".equalsIgnoreCase(msg.getRole()) ? "user" : "assistant";
                    messages.add(Map.of("role", role, "content", msg.getContent()));
                }
            }
        }

        messages.add(Map.of("role", "user", "content", userMessage));
        String responseText = callOpenAIWithMessages(messages);
        log.info("[OPENAI SERVICE] Successfully received response: '{}'",
                responseText != null && responseText.length() > 100 ? responseText.substring(0, 100) + "..." : responseText);
        return responseText;
    }

    private String callOpenAIWithMessages(java.util.List<Map<String, Object>> messages) {
        Map<String, Object> requestBody = Map.of(
                "model", aiConfig.getModel() != null ? aiConfig.getModel() : "gpt-3.5-turbo",
                "messages", messages,
                "temperature", 0.7,
                "max_tokens", 800
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getApiKey());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        int maxRetries = aiConfig.getMaxRetries();
        long backoff = 1000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                Map<String, Object> response = restTemplate.postForObject(
                        aiConfig.getApiUrl() != null ? aiConfig.getApiUrl() : "https://api.openai.com/v1/chat/completions",
                        requestEntity,
                        Map.class
                );
                
                if (response != null && response.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to call OpenAI chat on attempt {}: {}", i + 1, e.getMessage());
                if (i == maxRetries - 1) {
                    throw new RuntimeException("AIServiceException: Failed to call OpenAI after " + maxRetries + " attempts", e);
                }
                try {
                    Thread.sleep(backoff);
                    backoff *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during backoff", ie);
                }
            }
        }
        throw new RuntimeException("AIServiceException: Failed to get valid response");
    }

    private String callOpenAI(String systemMessage, String userMessage) {
        Map<String, Object> requestBody = Map.of(
                "model", aiConfig.getModel() != null ? aiConfig.getModel() : "gpt-3.5-turbo",
                "messages", List.of(
                        Map.of("role", "system", "content", systemMessage),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.7,
                "max_tokens", 1000
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiConfig.getApiKey());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        int maxRetries = aiConfig.getMaxRetries();
        long backoff = 1000;

        for (int i = 0; i < maxRetries; i++) {
            try {
                Map<String, Object> response = restTemplate.postForObject(
                        aiConfig.getApiUrl() != null ? aiConfig.getApiUrl() : "https://api.openai.com/v1/chat/completions",
                        requestEntity,
                        Map.class
                );
                
                if (response != null && response.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        return (String) message.get("content");
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to call OpenAI on attempt {}: {}", i + 1, e.getMessage());
                if (i == maxRetries - 1) {
                    throw new RuntimeException("AIServiceException: Failed to call OpenAI after " + maxRetries + " attempts", e);
                }
                try {
                    Thread.sleep(backoff);
                    backoff *= 2;
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during backoff", ie);
                }
            }
        }
        throw new RuntimeException("AIServiceException: Failed to get valid response");
    }

    private <T> T parseResponse(String json, Class<T> clazz) {
        try {
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.startsWith("```")) {
                json = json.substring(3);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            return objectMapper.readValue(json.trim(), clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("InvalidAIResponseException: Failed to parse JSON", e);
        }
    }
}
