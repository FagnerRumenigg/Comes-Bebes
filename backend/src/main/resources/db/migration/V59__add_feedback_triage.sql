ALTER TABLE application.feedback_submissions
    ADD COLUMN category varchar(20) NOT NULL DEFAULT 'SUGGESTION',
    ADD COLUMN status varchar(20) NOT NULL DEFAULT 'NEW';

ALTER TABLE application.feedback_submissions
    ADD CONSTRAINT feedback_submissions_category_ck
        CHECK (category IN ('SUGGESTION', 'BUG', 'QUESTION', 'CONTACT')),
    ADD CONSTRAINT feedback_submissions_status_ck
        CHECK (status IN ('NEW', 'IN_REVIEW', 'RESPONDED', 'ARCHIVED'));
