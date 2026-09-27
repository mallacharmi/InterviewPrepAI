CREATE TABLE IF NOT EXISTS interviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_role VARCHAR(255),
    topics VARCHAR(500),
    interview_type VARCHAR(50),
    difficulty VARCHAR(50),
    status VARCHAR(50) DEFAULT 'CREATED',
    total_questions INT NOT NULL,
    completed_questions INT DEFAULT 0,
    overall_score DOUBLE,
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME,
    CONSTRAINT fk_interviews_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_interview_user_id ON interviews (user_id);
CREATE INDEX idx_interview_created_at ON interviews (created_at);
