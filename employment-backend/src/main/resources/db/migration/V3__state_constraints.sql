ALTER TABLE companies ADD CONSTRAINT chk_company_approval
    CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED'));

ALTER TABLE job_postings ADD CONSTRAINT chk_job_status
    CHECK (status IN ('DRAFT', 'PENDING', 'PUBLISHED', 'CLOSED', 'REJECTED'));

ALTER TABLE applications ADD CONSTRAINT chk_application_status
    CHECK (status IN ('APPLIED', 'UNDER_REVIEW', 'DOCUMENT_PASSED', 'INTERVIEW', 'FINAL_PASSED', 'REJECTED', 'CANCELED'));

ALTER TABLE interviews ADD CONSTRAINT chk_interview_status
    CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELED'));

ALTER TABLE interviews ADD CONSTRAINT chk_interview_type
    CHECK (interview_type IN ('ONLINE', 'OFFLINE'));

ALTER TABLE employment_links ADD CONSTRAINT chk_link_status
    CHECK (status IN ('REQUESTED', 'COUNSELING', 'MATCHING', 'INTERVIEW', 'EMPLOYED', 'CLOSED'));

ALTER TABLE inquiries ADD CONSTRAINT chk_inquiry_status
    CHECK (status IN ('OPEN', 'ANSWERED'));
