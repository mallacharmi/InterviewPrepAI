CREATE TABLE IF NOT EXISTS answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    answer_text TEXT,
    score DOUBLE,
    technical_accuracy DOUBLE,
    completeness DOUBLE,
    clarity DOUBLE,
    feedback TEXT,
    missing_concepts TEXT,
    improvement_suggestion TEXT,
    created_at DATETIME,
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions (id),
    CONSTRAINT fk_answers_user FOREIGN KEY (user_id) REFERENCES users (id)
);
