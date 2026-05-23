CREATE TABLE IF NOT EXISTS request_history (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(100),
    method VARCHAR(10),
    url TEXT,
    request_body TEXT,
    response_status INTEGER,
    response_body TEXT,
    latency_ms BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(100),
    endpoint VARCHAR(200),
    method VARCHAR(10),
    request_body TEXT,
    response_status INTEGER,
    latency_ms BIGINT,
    rate_limit_remaining INTEGER,
    rate_limit_status VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW()
);
