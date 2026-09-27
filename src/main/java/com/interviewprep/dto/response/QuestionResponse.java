package com.interviewprep.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String questionText;
    private String topic;
    private String difficulty;
    private String questionType;
    private int sequenceNumber;
    private int displayQuestionNumber;
    private boolean isFollowUp;
}
