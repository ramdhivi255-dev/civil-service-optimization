-- =========================================================
-- Supabase (PostgreSQL) Database Schema & Initial Seed Data
-- Project: Civil Services Officer Posting & Transfer System
-- =========================================================

-- Drop existing tables if re-running script
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS transfer_orders CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS transfer_requests CASCADE;
DROP TABLE IF EXISTS transfer_policies CASCADE;
DROP TABLE IF EXISTS vacancies CASCADE;
DROP TABLE IF EXISTS officers CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. USERS TABLE
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('CIVIL_SERVICE_OFFICER', 'CADRE_ADMINISTRATOR', 'TRANSFER_COMMITTEE_MEMBER')),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Seed Users (Passwords hashed with SHA-256)
INSERT INTO users (user_id, username, password, name, email, role, status) VALUES
(1, 'admin', 'cb7bddc90ba5cc6afbc2f5c61919682c8915633837eaac3747344db7293443e7', 'Cadre Administrator', 'admin@civilservices.tn.gov.in', 'CADRE_ADMINISTRATOR', 'ACTIVE'),
(2, 'officer1', '7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7', 'Arun Kumar', 'arun.kumar@civilservices.tn.gov.in', 'CIVIL_SERVICE_OFFICER', 'ACTIVE'),
(3, 'officer2', '7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7', 'Priya Sharma', 'priya.sharma@civilservices.tn.gov.in', 'CIVIL_SERVICE_OFFICER', 'ACTIVE'),
(4, 'officer3', '7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7', 'Ravi Kumar', 'ravi.kumar@civilservices.tn.gov.in', 'CIVIL_SERVICE_OFFICER', 'ACTIVE'),
(5, 'officer4', '7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7', 'Meena Devi', 'meena.devi@civilservices.tn.gov.in', 'CIVIL_SERVICE_OFFICER', 'ACTIVE'),
(6, 'officer5', '7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7', 'Karthik Raj', 'karthik.raj@civilservices.tn.gov.in', 'CIVIL_SERVICE_OFFICER', 'ACTIVE'),
(7, 'committee1', '203fb45e01d0f509abf65e53410bcb7736809a2be454eba03a65b8677ab23631', 'Dr. S. Ramanathan', 'committee@civilservices.tn.gov.in', 'TRANSFER_COMMITTEE_MEMBER', 'ACTIVE');

SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));

-- 2. OFFICERS TABLE
CREATE TABLE officers (
    officer_id SERIAL PRIMARY KEY,
    user_id INT UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    employee_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    date_of_birth DATE,
    joining_date DATE,
    designation VARCHAR(100),
    department VARCHAR(100),
    cadre VARCHAR(100),
    current_posting VARCHAR(100),
    current_district VARCHAR(100),
    current_state VARCHAR(100) DEFAULT 'Tamil Nadu',
    years_in_current_posting INT DEFAULT 0,
    previous_transfer_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

INSERT INTO officers (officer_id, user_id, employee_id, name, email, phone, date_of_birth, joining_date, designation, department, cadre, current_posting, current_district, current_state, years_in_current_posting, previous_transfer_date, status) VALUES
(1, 2, 'CSO1001', 'Arun Kumar', 'arun.kumar@civilservices.tn.gov.in', '9876543210', '1985-05-12', '2010-08-01', 'District Collector', 'Revenue Administration', 'IAS', 'Madurai Collectorate', 'Madurai', 'Tamil Nadu', 6, '2020-07-15', 'ACTIVE'),
(2, 3, 'CSO1002', 'Priya Sharma', 'priya.sharma@civilservices.tn.gov.in', '9876543211', '1988-11-24', '2013-06-15', 'Joint Commissioner', 'Commercial Taxes', 'IAS', 'Salem Head Office', 'Salem', 'Tamil Nadu', 4, '2022-01-10', 'ACTIVE'),
(3, 4, 'CSO1003', 'Ravi Kumar', 'ravi.kumar@civilservices.tn.gov.in', '9876543212', '1990-03-30', '2016-01-20', 'Sub-Collector', 'Revenue Department', 'IAS', 'Trichy Office', 'Trichy', 'Tamil Nadu', 2, '2024-03-01', 'ACTIVE'),
(4, 5, 'CSO1004', 'Meena Devi', 'meena.devi@civilservices.tn.gov.in', '9876543213', '1987-09-08', '2012-09-10', 'Additional Collector', 'Rural Development', 'IAS', 'Erode Collectorate', 'Erode', 'Tamil Nadu', 5, '2021-08-20', 'ACTIVE'),
(5, 6, 'CSO1005', 'Karthik Raj', 'karthik.raj@civilservices.tn.gov.in', '9876543214', '1989-12-18', '2015-04-05', 'Deputy Commissioner', 'Transport', 'IAS', 'Tirunelveli Circle', 'Tirunelveli', 'Tamil Nadu', 3, '2023-05-11', 'ACTIVE');

SELECT setval('officers_officer_id_seq', (SELECT MAX(officer_id) FROM officers));

-- 3. VACANCIES TABLE
CREATE TABLE vacancies (
    vacancy_id SERIAL PRIMARY KEY,
    location VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    state VARCHAR(100) DEFAULT 'Tamil Nadu',
    department VARCHAR(100) NOT NULL,
    designation VARCHAR(100) NOT NULL,
    cadre VARCHAR(100) NOT NULL,
    available_positions INT NOT NULL DEFAULT 1,
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO vacancies (vacancy_id, location, district, state, department, designation, cadre, available_positions, status) VALUES
(1, 'Chennai Secretariat', 'Chennai', 'Tamil Nadu', 'Revenue Administration', 'District Collector', 'IAS', 3, 'AVAILABLE'),
(2, 'Coimbatore Civil Station', 'Coimbatore', 'Tamil Nadu', 'Commercial Taxes', 'Joint Commissioner', 'IAS', 2, 'AVAILABLE'),
(3, 'Madurai Collectorate', 'Madurai', 'Tamil Nadu', 'Rural Development', 'Additional Collector', 'IAS', 1, 'AVAILABLE'),
(4, 'Salem Head Office', 'Salem', 'Tamil Nadu', 'Transport', 'Deputy Commissioner', 'IAS', 4, 'AVAILABLE'),
(5, 'Trichy District Office', 'Trichy', 'Tamil Nadu', 'Revenue Department', 'Sub-Collector', 'IAS', 2, 'AVAILABLE');

SELECT setval('vacancies_vacancy_id_seq', (SELECT MAX(vacancy_id) FROM vacancies));

-- 4. TRANSFER POLICIES TABLE
CREATE TABLE transfer_policies (
    policy_id SERIAL PRIMARY KEY,
    policy_name VARCHAR(150) NOT NULL,
    description TEXT,
    minimum_service_years INT NOT NULL DEFAULT 3,
    maximum_transfer_frequency INT NOT NULL DEFAULT 1,
    priority_rules VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE'
);

INSERT INTO transfer_policies (policy_id, policy_name, description, minimum_service_years, maximum_transfer_frequency, priority_rules, status) VALUES
(1, 'General Service Tenure Policy', 'Officers must serve a minimum of 3 consecutive years in their current posting before requesting a routine transfer.', 3, 1, 'Standard seniority and posting duration rules apply.', 'ACTIVE'),
(2, 'Spouse & Family Co-Location Policy', 'Priority consideration given to officers requesting transfer for spouse co-location in government service.', 2, 1, 'Spouse employment proof required.', 'ACTIVE'),
(3, 'Medical & Special Care Policy', 'Immediate priority processing for officers or dependents requiring specialized medical treatment.', 1, 2, 'Medical board verification mandatory.', 'ACTIVE');

SELECT setval('transfer_policies_policy_id_seq', (SELECT MAX(policy_id) FROM transfer_policies));

-- 5. TRANSFER REQUESTS TABLE
CREATE TABLE transfer_requests (
    request_id SERIAL PRIMARY KEY,
    officer_id INT NOT NULL REFERENCES officers(officer_id) ON DELETE CASCADE,
    request_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    reason TEXT NOT NULL,
    preferred_location_1 VARCHAR(100) NOT NULL,
    preferred_location_2 VARCHAR(100),
    preferred_location_3 VARCHAR(100),
    status VARCHAR(30) DEFAULT 'SUBMITTED',
    eligibility_status VARCHAR(30) DEFAULT 'PENDING_VERIFICATION',
    optimization_score INT DEFAULT 0,
    priority VARCHAR(20) DEFAULT 'LOW',
    remarks TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO transfer_requests (request_id, officer_id, reason, preferred_location_1, preferred_location_2, preferred_location_3, status, eligibility_status, optimization_score, priority, remarks) VALUES
(1, 1, 'Medical reasons and family care in capital city.', 'Chennai', 'Coimbatore', 'Salem', 'APPROVED', 'ELIGIBLE', 93, 'HIGH', 'Verified medical certificate. Eligible and recommended for approval.'),
(2, 2, 'Spouse working in Coimbatore District Court.', 'Coimbatore', 'Chennai', 'Madurai', 'COMMITTEE_REVIEW', 'ELIGIBLE', 78, 'HIGH', 'Documents verified. Forwarded to committee for final review.'),
(3, 3, 'Personal request for location change.', 'Chennai', 'Coimbatore', 'Thanjavur', 'SUBMITTED', 'PENDING_VERIFICATION', 45, 'LOW', 'Initial submission awaiting administrator document verification.');

SELECT setval('transfer_requests_request_id_seq', (SELECT MAX(request_id) FROM transfer_requests));

-- 6. DOCUMENTS TABLE
CREATE TABLE documents (
    document_id SERIAL PRIMARY KEY,
    request_id INT NOT NULL REFERENCES transfer_requests(request_id) ON DELETE CASCADE,
    document_name VARCHAR(150) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    document_url VARCHAR(255),
    file_path VARCHAR(255),
    verification_status VARCHAR(20) DEFAULT 'PENDING',
    verified_by INT REFERENCES users(user_id),
    verification_comments TEXT,
    uploaded_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO documents (document_id, request_id, document_name, document_type, document_url, file_path, verification_status, verified_by, verification_comments) VALUES
(1, 1, 'Medical_Certificate_ArunKumar.pdf', 'Medical', 'uploads/docs/medical_cso1001.pdf', 'uploads/docs/medical_cso1001.pdf', 'VERIFIED', 1, 'Verified by Admin: Valid hospital document'),
(2, 2, 'Spouse_Employment_Certificate.pdf', 'Spouse', 'uploads/docs/spouse_cso1002.pdf', 'uploads/docs/spouse_cso1002.pdf', 'VERIFIED', 1, 'Verified by Admin: Employment certificate confirmed'),
(3, 3, 'Personal_Representation_Letter.pdf', 'Personal', 'uploads/docs/personal_cso1003.pdf', 'uploads/docs/personal_cso1003.pdf', 'PENDING', NULL, NULL);

SELECT setval('documents_document_id_seq', (SELECT MAX(document_id) FROM documents));

-- 7. REVIEWS TABLE
CREATE TABLE reviews (
    review_id SERIAL PRIMARY KEY,
    request_id INT NOT NULL REFERENCES transfer_requests(request_id) ON DELETE CASCADE,
    reviewer_id INT NOT NULL REFERENCES users(user_id),
    recommendation VARCHAR(30) NOT NULL,
    remarks TEXT,
    review_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO reviews (review_id, request_id, reviewer_id, recommendation, remarks) VALUES
(1, 1, 7, 'APPROVE', 'Officer has served 6 years in current posting. High priority medical case with available vacancy in Chennai.'),
(2, 2, 7, 'APPROVE', 'Spouse co-location valid case. Recommend approval for Coimbatore posting.');

SELECT setval('reviews_review_id_seq', (SELECT MAX(review_id) FROM reviews));

-- 8. TRANSFER ORDERS TABLE
CREATE TABLE transfer_orders (
    order_id SERIAL PRIMARY KEY,
    request_id INT UNIQUE NOT NULL REFERENCES transfer_requests(request_id) ON DELETE CASCADE,
    officer_id INT NOT NULL REFERENCES officers(officer_id) ON DELETE CASCADE,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    old_location VARCHAR(100) NOT NULL,
    new_location VARCHAR(100) NOT NULL,
    order_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    joining_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'GENERATED'
);

INSERT INTO transfer_orders (order_id, request_id, officer_id, order_number, old_location, new_location, joining_date, status) VALUES
(1, 1, 1, 'GO-TN-REV-2026-0941', 'Madurai Collectorate', 'Chennai Secretariat', '2026-09-15', 'ACKNOWLEDGED');

SELECT setval('transfer_orders_order_id_seq', (SELECT MAX(order_id) FROM transfer_orders));

-- 9. NOTIFICATIONS TABLE
CREATE TABLE notifications (
    notification_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) DEFAULT 'INFO',
    read_status VARCHAR(20) DEFAULT 'UNREAD',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO notifications (notification_id, user_id, title, message, notification_type, read_status) VALUES
(1, 2, 'Transfer Request Approved', 'Your transfer request (REQ-1) to Chennai Secretariat has been approved by the Transfer Committee.', 'SUCCESS', 'UNREAD'),
(2, 2, 'Transfer Order Generated', 'Official Transfer Order GO-TN-REV-2026-0941 has been issued. Please review and acknowledge.', 'ACTION_REQUIRED', 'UNREAD'),
(3, 3, 'Request Under Committee Review', 'Your transfer request (REQ-2) has been forwarded to the Transfer Committee for review.', 'INFO', 'READ'),
(4, 1, 'New Transfer Request Submitted', 'New request #4 submitted by Officer.', 'INFO', 'UNREAD');

SELECT setval('notifications_notification_id_seq', (SELECT MAX(notification_id) FROM notifications));

-- 10. AUDIT LOGS TABLE
CREATE TABLE audit_logs (
    log_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id) ON DELETE SET NULL,
    role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(50) NOT NULL,
    entity_id INT,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO audit_logs (log_id, user_id, role, action, entity, entity_id, description) VALUES
(1, 2, 'CIVIL_SERVICE_OFFICER', 'SUBMIT_REQUEST', 'transfer_requests', 1, 'Officer Arun Kumar submitted a transfer request for Chennai.'),
(2, 1, 'CADRE_ADMINISTRATOR', 'VERIFY_DOCUMENT', 'documents', 1, 'Administrator verified medical document for Request #1.'),
(3, 7, 'TRANSFER_COMMITTEE_MEMBER', 'APPROVE_REQUEST', 'transfer_requests', 1, 'Transfer Committee Member approved Request #1.'),
(4, 1, 'CADRE_ADMINISTRATOR', 'GENERATE_ORDER', 'transfer_orders', 1, 'Transfer Order GO-TN-REV-2026-0941 generated for Officer Arun Kumar.');

SELECT setval('audit_logs_log_id_seq', (SELECT MAX(log_id) FROM audit_logs));
