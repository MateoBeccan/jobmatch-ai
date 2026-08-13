CREATE TABLE IF NOT EXISTS analyses (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(120) NOT NULL,
    cv_file_name VARCHAR(160) NOT NULL,
    cv_version VARCHAR(120) NOT NULL,
    role VARCHAR(160) NOT NULL,
    company VARCHAR(120) NOT NULL,
    job_description CLOB NOT NULL,
    mode VARCHAR(16) NOT NULL,
    score INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    result_json CLOB NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_analyses_owner_created_at ON analyses(owner_id, created_at DESC);
