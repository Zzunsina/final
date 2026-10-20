CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255) NOT NULL UNIQUE,
        password VARCHAR(100) NOT NULL, name VARCHAR(100) NOT NULL, phone VARCHAR(11) NOT NULL,
        role VARCHAR(20) NOT NULL CHECK (role IN ('JOB_SEEKER','COMPANY','ADMIN')),
        status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED','WITHDRAWN')),
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_seekers (
    job_seeker_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT NOT NULL UNIQUE, FOREIGN KEY (user_id) REFERENCES users(user_id), birth_date DATE,
        address VARCHAR(255), desired_job VARCHAR(100), desired_region VARCHAR(100),
        experience_years INT DEFAULT 0 CHECK (experience_years BETWEEN 0 AND 80), introduction TEXT
);

CREATE TABLE companies (
    company_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT NOT NULL UNIQUE, FOREIGN KEY (user_id) REFERENCES users(user_id), company_name VARCHAR(200) NOT NULL,
        business_number VARCHAR(10) NOT NULL UNIQUE, ceo_name VARCHAR(100), industry VARCHAR(100),
        company_size VARCHAR(100), address VARCHAR(255), website VARCHAR(500), description TEXT,
        approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', review_reason TEXT,
        reviewed_by BIGINT, FOREIGN KEY (reviewed_by) REFERENCES users(user_id), reviewed_at TIMESTAMP
);

CREATE TABLE job_postings (
    job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        company_id BIGINT NOT NULL, FOREIGN KEY (company_id) REFERENCES companies(company_id), title VARCHAR(200) NOT NULL,
        job_type VARCHAR(50) NOT NULL, career_type VARCHAR(50) NOT NULL, education VARCHAR(100),
        salary VARCHAR(100), work_location VARCHAR(255) NOT NULL, description TEXT NOT NULL,
        requirements TEXT, preferred_qualifications TEXT,
        headcount INT NOT NULL CHECK (headcount BETWEEN 1 AND 10000), start_date DATE NOT NULL,
        end_date DATE NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', review_reason TEXT,
        reviewed_by BIGINT, FOREIGN KEY (reviewed_by) REFERENCES users(user_id), reviewed_at TIMESTAMP,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, CHECK (start_date <= end_date)
);

CREATE TABLE resumes (
    resume_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        job_seeker_id BIGINT NOT NULL, FOREIGN KEY (job_seeker_id) REFERENCES job_seekers(job_seeker_id), title VARCHAR(200) NOT NULL,
        education VARCHAR(100), career_description TEXT, skills TEXT, portfolio_url VARCHAR(500),
        is_public BOOLEAN NOT NULL DEFAULT FALSE, is_default BOOLEAN NOT NULL DEFAULT FALSE,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_bookmarks (
    bookmark_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        job_seeker_id BIGINT NOT NULL, FOREIGN KEY (job_seeker_id) REFERENCES job_seekers(job_seeker_id),
        job_id BIGINT NOT NULL, FOREIGN KEY (job_id) REFERENCES job_postings(job_id),
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(job_seeker_id,job_id)
);

CREATE TABLE applications (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        job_id BIGINT NOT NULL, FOREIGN KEY (job_id) REFERENCES job_postings(job_id),
        job_seeker_id BIGINT NOT NULL, FOREIGN KEY (job_seeker_id) REFERENCES job_seekers(job_seeker_id),
        resume_id BIGINT NOT NULL, FOREIGN KEY (resume_id) REFERENCES resumes(resume_id), cover_letter TEXT,
        status VARCHAR(30) NOT NULL DEFAULT 'APPLIED',
        applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(job_id,job_seeker_id)
);

CREATE TABLE interviews (
    interview_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        application_id BIGINT NOT NULL, FOREIGN KEY (application_id) REFERENCES applications(application_id),
        interview_at TIMESTAMP NOT NULL, interview_type VARCHAR(20) NOT NULL,
        location VARCHAR(255), meeting_url VARCHAR(500), status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
        result TEXT, memo TEXT, request_key VARCHAR(100) NOT NULL, request_hash VARCHAR(64) NOT NULL,
        UNIQUE(application_id,request_key)
);

CREATE TABLE employment_links (
    employment_link_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        job_seeker_id BIGINT NOT NULL, FOREIGN KEY (job_seeker_id) REFERENCES job_seekers(job_seeker_id),
        company_id BIGINT, FOREIGN KEY (company_id) REFERENCES companies(company_id), job_id BIGINT, FOREIGN KEY (job_id) REFERENCES job_postings(job_id),
        manager_user_id BIGINT, FOREIGN KEY (manager_user_id) REFERENCES users(user_id), status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
        memo TEXT, public_message TEXT, requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        completed_at TIMESTAMP
);

CREATE TABLE notices (
    notice_id BIGINT AUTO_INCREMENT PRIMARY KEY, title VARCHAR(200) NOT NULL,
        content TEXT NOT NULL, author_user_id BIGINT NOT NULL, FOREIGN KEY (author_user_id) REFERENCES users(user_id),
        is_published BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inquiries (
    inquiry_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, FOREIGN KEY (user_id) REFERENCES users(user_id),
        title VARCHAR(200) NOT NULL, content TEXT NOT NULL, answer TEXT, status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
        answered_by BIGINT, FOREIGN KEY (answered_by) REFERENCES users(user_id), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        answered_at TIMESTAMP
);

CREATE TABLE notifications (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT NOT NULL, FOREIGN KEY (user_id) REFERENCES users(user_id), title VARCHAR(200) NOT NULL,
        content VARCHAR(1000) NOT NULL, notification_type VARCHAR(50), related_id BIGINT,
        is_read BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
