package com.interviewprep.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.dto.request.AnswerSubmitRequest;
import com.interviewprep.dto.request.InterviewCreateRequest;
import com.interviewprep.entity.Interview;
import com.interviewprep.entity.Question;
import com.interviewprep.entity.User;
import com.interviewprep.entity.enums.Difficulty;
import com.interviewprep.entity.enums.ExperienceLevel;
import com.interviewprep.entity.enums.InterviewStatus;
import com.interviewprep.entity.enums.InterviewType;
import com.interviewprep.repository.InterviewRepository;
import com.interviewprep.repository.QuestionRepository;
import com.interviewprep.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InterviewApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private User testUser;
    private Interview testInterview;
    private Question testQuestion;

    @BeforeEach
    void setUp() {
        questionRepository.deleteAll();
        interviewRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("user@example.com");
        testUser.setPassword("password123");
        testUser.setTargetRole("Developer");
        testUser.setExperienceLevel(ExperienceLevel.JUNIOR);
        testUser = userRepository.save(testUser);
        
        testInterview = new Interview();
        testInterview.setUser(testUser);
        testInterview.setTargetRole("Developer");
        testInterview.setTopics("Java,Spring");
        testInterview.setInterviewType(InterviewType.TECHNICAL);
        testInterview.setDifficulty(Difficulty.MEDIUM);
        testInterview.setStatus(InterviewStatus.IN_PROGRESS);
        testInterview.setTotalQuestions(3);
        testInterview.setCompletedQuestions(0);
        testInterview.setCreatedAt(LocalDateTime.now());
        testInterview = interviewRepository.save(testInterview);
        
        testQuestion = new Question();
        testQuestion.setInterview(testInterview);
        testQuestion.setQuestionText("What is Java?");
        testQuestion.setSequenceNumber(1);
        testQuestion.setCreatedAt(LocalDateTime.now());
        testQuestion = questionRepository.save(testQuestion);
    }

    @Test
    @WithUserDetails(value = "user@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void createInterview_Success() throws Exception {
        InterviewCreateRequest request = new InterviewCreateRequest();
        request.setTargetRole("Senior Developer");
        request.setInterviewType("TECHNICAL");
        request.setDifficulty("HARD");
        request.setTotalQuestions(5);
        request.setTopics(List.of("Java", "Spring"));

        mockMvc.perform(post("/api/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(InterviewStatus.CREATED.name()));
    }

    @Test
    void createInterview_Unauthenticated() throws Exception {
        InterviewCreateRequest request = new InterviewCreateRequest();
        request.setTargetRole("Senior Developer");
        request.setInterviewType("TECHNICAL");
        request.setDifficulty("HARD");
        request.setTotalQuestions(5);

        mockMvc.perform(post("/api/interviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithUserDetails(value = "user@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getInterviews_Success() throws Exception {
        mockMvc.perform(get("/api/interviews"))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "user@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void startInterview_Success() throws Exception {
        Interview newInterview = new Interview();
        newInterview.setUser(testUser);
        newInterview.setTargetRole("Developer");
        newInterview.setTopics("Java,Spring");
        newInterview.setInterviewType(InterviewType.TECHNICAL);
        newInterview.setDifficulty(Difficulty.MEDIUM);
        newInterview.setStatus(InterviewStatus.CREATED);
        newInterview.setTotalQuestions(3);
        newInterview.setCreatedAt(LocalDateTime.now());
        newInterview = interviewRepository.save(newInterview);

        mockMvc.perform(post("/api/interviews/" + newInterview.getId() + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithUserDetails(value = "user@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void submitAnswer_Success() throws Exception {
        AnswerSubmitRequest request = new AnswerSubmitRequest();
        request.setAnswerText("Java is a high-level, class-based object-oriented programming language.");

        mockMvc.perform(post("/api/interviews/" + testInterview.getId() + "/questions/" + testQuestion.getId() + "/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").exists());
    }

    @Test
    @WithUserDetails(value = "user@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void completeInterview_Success() throws Exception {
        mockMvc.perform(post("/api/interviews/" + testInterview.getId() + "/complete"))
                .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(value = "user@example.com", setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void getReport_Success() throws Exception {
        Interview freshInterview = interviewRepository.findById(testInterview.getId()).orElseThrow();
        freshInterview.setStatus(InterviewStatus.COMPLETED);
        interviewRepository.save(freshInterview);

        mockMvc.perform(get("/api/interviews/" + testInterview.getId() + "/report"))
                .andExpect(status().isOk());
    }
}
