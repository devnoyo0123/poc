CREATE TABLE queue_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    wait_time_seconds INTEGER,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_queue_history_user_id ON queue_history(user_id);
CREATE INDEX idx_queue_history_status ON queue_history(status);
CREATE INDEX idx_queue_history_joined_at ON queue_history(joined_at);
