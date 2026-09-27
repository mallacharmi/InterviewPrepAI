package com.interviewprep.config;

import com.interviewprep.service.ai.AIService;
import com.interviewprep.service.ai.MockAIService;
import com.interviewprep.service.ai.OpenAIService;
import com.interviewprep.service.ai.PromptBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AIServiceConfig {

    @Bean
    public AIService aiService(AIConfig aiConfig, PromptBuilder promptBuilder, @Qualifier("aiRestTemplate") RestTemplate restTemplate) {
        if (aiConfig.getApiKey() != null && !aiConfig.getApiKey().isBlank()) {
            return new OpenAIService(aiConfig, promptBuilder, restTemplate);
        }
        return new MockAIService();
    }
}
