CREATE TABLE IF NOT EXISTS performance_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    interview_id BIGINT NOT NULL,
    topic VARCHAR(255),
    score DOUBLE,
    created_at DATETIME,
    CONSTRAINT fk_performance_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_performance_interview FOREIGN KEY (interview_id) REFERENCES interviews (id)
);
