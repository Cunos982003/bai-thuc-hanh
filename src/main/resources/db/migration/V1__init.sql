CREATE TABLE companies (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    website VARCHAR(255)
);

CREATE TABLE jobs (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    salary_min  BIGINT,
    salary_max  BIGINT,
    location    VARCHAR(255),
    status      VARCHAR(16) NOT NULL,
    company_id  BIGINT NOT NULL REFERENCES companies (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_jobs_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT chk_jobs_salary CHECK (
        salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max
    )
);

CREATE TABLE applications (
    id         BIGSERIAL PRIMARY KEY,
    job_id     BIGINT NOT NULL REFERENCES jobs (id),
    full_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    cv_url     VARCHAR(1000) NOT NULL,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_applications_job_email UNIQUE (job_id, email)
);

CREATE INDEX idx_jobs_location ON jobs (location);
CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_company_id ON jobs (company_id);
CREATE INDEX idx_applications_job_id ON applications (job_id);
