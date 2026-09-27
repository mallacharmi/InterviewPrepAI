CREATE TABLE IF NOT EXISTS questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    interview_id BIGINT NOT NULL,
    question_text TEXT,
    topic VARCHAR(255),
    difficulty VARCHAR(50),
    question_type VARCHAR(50),
    sequence_number INT NOT NULL,
    expected_concepts TEXT,
    is_follow_up BOOLEAN DEFAULT FALSE,
    parent_question_id BIGINT,
    created_at DATETIME,
    CONSTRAINT fk_questions_interview FOREIGN KEY (interview_id) REFERENCES interviews (id)
);

CREATE INDEX idx_question_interview_id ON questions (interview_id);
