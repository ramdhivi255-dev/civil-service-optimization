package com.example;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static boolean initialized = false;

    private static String getEnv(String primaryKey, String secondaryKey, String tertiaryKey, String defaultValue) {
        String val = System.getenv(primaryKey);
        if (val != null && !val.trim().isEmpty()) return val.trim();
        val = System.getenv(secondaryKey);
        if (val != null && !val.trim().isEmpty()) return val.trim();
        val = System.getenv(tertiaryKey);
        if (val != null && !val.trim().isEmpty()) return val.trim();
        return defaultValue;
    }

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                // Driver registered via ServiceLoader / JDBC 4.0
            }
        }
    }

    public static synchronized Connection getConnection() throws SQLException {
        String dbType = getEnv("DB_TYPE", "DB_VENDOR", "DB_DRIVER", "sqlite").toLowerCase();
        
        Connection conn;
        if (dbType.contains("mysql")) {
            String host = getEnv("DB_HOST", "MYSQLHOST", "MYSQL_HOST", "localhost");
            String port = getEnv("DB_PORT", "MYSQLPORT", "MYSQL_PORT", "3306");
            String dbName = getEnv("DB_NAME", "MYSQLDATABASE", "MYSQL_DATABASE", "civil_services_transfer_system");
            String user = getEnv("DB_USER", "MYSQLUSER", "MYSQL_USER", "root");
            String password = getEnv("DB_PASSWORD", "MYSQLPASSWORD", "MYSQL_PASSWORD", "root");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            conn = DriverManager.getConnection(url, user, password);
        } else {
            // SQLite Local Storage Default
            String dbPath = getEnv("SQLITE_DB_PATH", "DB_PATH", "DB_FILE", "civil_services.db");
            String url = "jdbc:sqlite:" + dbPath;
            conn = DriverManager.getConnection(url);
            
            if (!initialized) {
                initSQLiteDatabase(conn);
                initialized = true;
            }
        }

        return conn;
    }

    private static void initSQLiteDatabase(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");

            // Check if users table exists
            boolean tablesExist = false;
            try (java.sql.ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='users';")) {
                if (rs.next()) {
                    tablesExist = true;
                }
            }

            if (!tablesExist) {
                System.out.println("Initializing SQLite Database Tables & Seed Data...");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT NOT NULL, " +
                    "role TEXT NOT NULL CHECK (role IN ('CIVIL_SERVICE_OFFICER', 'CADRE_ADMINISTRATOR', 'TRANSFER_COMMITTEE_MEMBER')), " +
                    "status TEXT DEFAULT 'ACTIVE', " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

                stmt.executeUpdate("INSERT OR IGNORE INTO users (user_id, username, password, name, email, role, status) VALUES " +
                    "(1, 'admin', 'cb7bddc90ba5cc6afbc2f5c61919682c8915633837eaac3747344db7293443e7', 'Cadre Administrator', 'admin@civilservices.tn.gov.in', 'CADRE_ADMINISTRATOR', 'ACTIVE'), " +
                    "(2, 'officer1', '7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7', 'Arun Kumar', 'arun.kumar@civilservices.tn.gov.in', 'CIVIL_SERVICE_OFFICER', 'ACTIVE'), " +
                    "(3, 'officer2', '7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7', 'Priya Sharma', 'priya.sharma@civilservices.tn.gov.in', 'CIVIL_SERVICE_OFFICER', 'ACTIVE'), " +
                    "(4, 'officer3', '7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7', 'Ravi Kumar', 'ravi.kumar@civilservices.tn.gov.in', 'CIVIL_SERVICE_OFFICER', 'ACTIVE'), " +
                    "(5, 'officer4', '7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7', 'Meena Devi', 'meena.devi@civilservices.tn.gov.in', 'CIVIL_SERVICE_OFFICER', 'ACTIVE'), " +
                    "(6, 'officer5', '7ed570e8eb4d416c056d505332b9b5c88aa0afc07ae939c90c2bc4842b445ae7', 'Karthik Raj', 'karthik.raj@civilservices.tn.gov.in', 'CIVIL_SERVICE_OFFICER', 'ACTIVE'), " +
                    "(7, 'committee1', '203fb45e01d0f509abf65e53410bcb7736809a2be454eba03a65b8677ab23631', 'Dr. S. Ramanathan', 'committee@civilservices.tn.gov.in', 'TRANSFER_COMMITTEE_MEMBER', 'ACTIVE');");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS officers (" +
                    "officer_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER UNIQUE NOT NULL REFERENCES users(user_id) ON DELETE CASCADE, " +
                    "employee_id TEXT UNIQUE NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT NOT NULL, " +
                    "phone TEXT, " +
                    "date_of_birth DATE, " +
                    "joining_date DATE, " +
                    "designation TEXT, " +
                    "department TEXT, " +
                    "cadre TEXT, " +
                    "current_posting TEXT, " +
                    "current_district TEXT, " +
                    "current_state TEXT DEFAULT 'Tamil Nadu', " +
                    "years_in_current_posting INTEGER DEFAULT 0, " +
                    "previous_transfer_date DATE, " +
                    "status TEXT DEFAULT 'ACTIVE');");

                stmt.executeUpdate("INSERT OR IGNORE INTO officers (officer_id, user_id, employee_id, name, email, phone, date_of_birth, joining_date, designation, department, cadre, current_posting, current_district, current_state, years_in_current_posting, previous_transfer_date, status) VALUES " +
                    "(1, 2, 'CSO1001', 'Arun Kumar', 'arun.kumar@civilservices.tn.gov.in', '9876543210', '1985-05-12', '2010-08-01', 'District Collector', 'Revenue Administration', 'IAS', 'Madurai Collectorate', 'Madurai', 'Tamil Nadu', 6, '2020-07-15', 'ACTIVE'), " +
                    "(2, 3, 'CSO1002', 'Priya Sharma', 'priya.sharma@civilservices.tn.gov.in', '9876543211', '1988-11-24', '2013-06-15', 'Joint Commissioner', 'Commercial Taxes', 'IAS', 'Salem Head Office', 'Salem', 'Tamil Nadu', 4, '2022-01-10', 'ACTIVE'), " +
                    "(3, 4, 'CSO1003', 'Ravi Kumar', 'ravi.kumar@civilservices.tn.gov.in', '9876543212', '1990-03-30', '2016-01-20', 'Sub-Collector', 'Revenue Department', 'IAS', 'Trichy Office', 'Trichy', 'Tamil Nadu', 2, '2024-03-01', 'ACTIVE'), " +
                    "(4, 5, 'CSO1004', 'Meena Devi', 'meena.devi@civilservices.tn.gov.in', '9876543213', '1987-09-08', '2012-09-10', 'Additional Collector', 'Rural Development', 'IAS', 'Erode Collectorate', 'Erode', 'Tamil Nadu', 5, '2021-08-20', 'ACTIVE'), " +
                    "(5, 6, 'CSO1005', 'Karthik Raj', 'karthik.raj@civilservices.tn.gov.in', '9876543214', '1989-12-18', '2015-04-05', 'Deputy Commissioner', 'Transport', 'IAS', 'Tirunelveli Circle', 'Tirunelveli', 'Tamil Nadu', 3, '2023-05-11', 'ACTIVE');");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS vacancies (" +
                    "vacancy_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "location TEXT NOT NULL, " +
                    "district TEXT NOT NULL, " +
                    "state TEXT DEFAULT 'Tamil Nadu', " +
                    "department TEXT NOT NULL, " +
                    "designation TEXT NOT NULL, " +
                    "cadre TEXT NOT NULL, " +
                    "available_positions INTEGER NOT NULL DEFAULT 1, " +
                    "status TEXT DEFAULT 'AVAILABLE', " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

                stmt.executeUpdate("INSERT OR IGNORE INTO vacancies (vacancy_id, location, district, state, department, designation, cadre, available_positions, status) VALUES " +
                    "(1, 'Chennai Secretariat', 'Chennai', 'Tamil Nadu', 'Revenue Administration', 'District Collector', 'IAS', 3, 'AVAILABLE'), " +
                    "(2, 'Coimbatore Civil Station', 'Coimbatore', 'Tamil Nadu', 'Commercial Taxes', 'Joint Commissioner', 'IAS', 2, 'AVAILABLE'), " +
                    "(3, 'Madurai Collectorate', 'Madurai', 'Tamil Nadu', 'Rural Development', 'Additional Collector', 'IAS', 1, 'AVAILABLE'), " +
                    "(4, 'Salem Head Office', 'Salem', 'Tamil Nadu', 'Transport', 'Deputy Commissioner', 'IAS', 4, 'AVAILABLE'), " +
                    "(5, 'Trichy District Office', 'Trichy', 'Tamil Nadu', 'Revenue Department', 'Sub-Collector', 'IAS', 2, 'AVAILABLE');");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transfer_policies (" +
                    "policy_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "policy_name TEXT NOT NULL, " +
                    "description TEXT, " +
                    "minimum_service_years INTEGER NOT NULL DEFAULT 3, " +
                    "maximum_transfer_frequency INTEGER NOT NULL DEFAULT 1, " +
                    "priority_rules TEXT, " +
                    "status TEXT DEFAULT 'ACTIVE');");

                stmt.executeUpdate("INSERT OR IGNORE INTO transfer_policies (policy_id, policy_name, description, minimum_service_years, maximum_transfer_frequency, priority_rules, status) VALUES " +
                    "(1, 'General Service Tenure Policy', 'Officers must serve a minimum of 3 consecutive years in their current posting before requesting a routine transfer.', 3, 1, 'Standard seniority and posting duration rules apply.', 'ACTIVE'), " +
                    "(2, 'Spouse & Family Co-Location Policy', 'Priority consideration given to officers requesting transfer for spouse co-location in government service.', 2, 1, 'Spouse employment proof required.', 'ACTIVE'), " +
                    "(3, 'Medical & Special Care Policy', 'Immediate priority processing for officers or dependents requiring specialized medical treatment.', 1, 2, 'Medical board verification mandatory.', 'ACTIVE');");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transfer_requests (" +
                    "request_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "officer_id INTEGER NOT NULL REFERENCES officers(officer_id) ON DELETE CASCADE, " +
                    "request_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "reason TEXT NOT NULL, " +
                    "preferred_location_1 TEXT NOT NULL, " +
                    "preferred_location_2 TEXT, " +
                    "preferred_location_3 TEXT, " +
                    "status TEXT DEFAULT 'SUBMITTED', " +
                    "eligibility_status TEXT DEFAULT 'PENDING_VERIFICATION', " +
                    "optimization_score INTEGER DEFAULT 0, " +
                    "priority TEXT DEFAULT 'LOW', " +
                    "remarks TEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

                stmt.executeUpdate("INSERT OR IGNORE INTO transfer_requests (request_id, officer_id, reason, preferred_location_1, preferred_location_2, preferred_location_3, status, eligibility_status, optimization_score, priority, remarks) VALUES " +
                    "(1, 1, 'Medical reasons and family care in capital city.', 'Chennai', 'Coimbatore', 'Salem', 'APPROVED', 'ELIGIBLE', 93, 'HIGH', 'Verified medical certificate. Eligible and recommended for approval.'), " +
                    "(2, 2, 'Spouse working in Coimbatore District Court.', 'Coimbatore', 'Chennai', 'Madurai', 'COMMITTEE_REVIEW', 'ELIGIBLE', 78, 'HIGH', 'Documents verified. Forwarded to committee for final review.'), " +
                    "(3, 3, 'Personal request for location change.', 'Chennai', 'Coimbatore', 'Thanjavur', 'SUBMITTED', 'PENDING_VERIFICATION', 45, 'LOW', 'Initial submission awaiting administrator document verification.');");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS documents (" +
                    "document_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "request_id INTEGER NOT NULL REFERENCES transfer_requests(request_id) ON DELETE CASCADE, " +
                    "document_name TEXT NOT NULL, " +
                    "document_type TEXT NOT NULL, " +
                    "document_url TEXT, " +
                    "file_path TEXT, " +
                    "verification_status TEXT DEFAULT 'PENDING', " +
                    "verified_by INTEGER REFERENCES users(user_id), " +
                    "verification_comments TEXT, " +
                    "uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

                stmt.executeUpdate("INSERT OR IGNORE INTO documents (document_id, request_id, document_name, document_type, document_url, file_path, verification_status, verified_by, verification_comments) VALUES " +
                    "(1, 1, 'Medical_Certificate_ArunKumar.pdf', 'Medical', 'uploads/docs/medical_cso1001.pdf', 'uploads/docs/medical_cso1001.pdf', 'VERIFIED', 1, 'Verified by Admin: Valid hospital document'), " +
                    "(2, 2, 'Spouse_Employment_Certificate.pdf', 'Spouse', 'uploads/docs/spouse_cso1002.pdf', 'uploads/docs/spouse_cso1002.pdf', 'VERIFIED', 1, 'Verified by Admin: Employment certificate confirmed'), " +
                    "(3, 3, 'Personal_Representation_Letter.pdf', 'Personal', 'uploads/docs/personal_cso1003.pdf', 'uploads/docs/personal_cso1003.pdf', 'PENDING', NULL, NULL);");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS reviews (" +
                    "review_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "request_id INTEGER NOT NULL REFERENCES transfer_requests(request_id) ON DELETE CASCADE, " +
                    "reviewer_id INTEGER NOT NULL REFERENCES users(user_id), " +
                    "recommendation TEXT NOT NULL, " +
                    "remarks TEXT, " +
                    "review_date DATETIME DEFAULT CURRENT_TIMESTAMP);");

                stmt.executeUpdate("INSERT OR IGNORE INTO reviews (review_id, request_id, reviewer_id, recommendation, remarks) VALUES " +
                    "(1, 1, 7, 'APPROVE', 'Officer has served 6 years in current posting. High priority medical case with available vacancy in Chennai.'), " +
                    "(2, 2, 7, 'APPROVE', 'Spouse co-location valid case. Recommend approval for Coimbatore posting.');");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transfer_orders (" +
                    "order_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "request_id INTEGER UNIQUE NOT NULL REFERENCES transfer_requests(request_id) ON DELETE CASCADE, " +
                    "officer_id INTEGER NOT NULL REFERENCES officers(officer_id) ON DELETE CASCADE, " +
                    "order_number TEXT UNIQUE NOT NULL, " +
                    "old_location TEXT NOT NULL, " +
                    "new_location TEXT NOT NULL, " +
                    "order_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "joining_date DATE NOT NULL, " +
                    "status TEXT DEFAULT 'GENERATED');");

                stmt.executeUpdate("INSERT OR IGNORE INTO transfer_orders (order_id, request_id, officer_id, order_number, old_location, new_location, joining_date, status) VALUES " +
                    "(1, 1, 1, 'GO-TN-REV-2026-0941', 'Madurai Collectorate', 'Chennai Secretariat', '2026-09-15', 'ACKNOWLEDGED');");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS notifications (" +
                    "notification_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE, " +
                    "title TEXT NOT NULL, " +
                    "message TEXT NOT NULL, " +
                    "notification_type TEXT DEFAULT 'INFO', " +
                    "read_status TEXT DEFAULT 'UNREAD', " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

                stmt.executeUpdate("INSERT OR IGNORE INTO notifications (notification_id, user_id, title, message, notification_type, read_status) VALUES " +
                    "(1, 2, 'Transfer Request Approved', 'Your transfer request (REQ-1) to Chennai Secretariat has been approved by the Transfer Committee.', 'SUCCESS', 'UNREAD'), " +
                    "(2, 2, 'Transfer Order Generated', 'Official Transfer Order GO-TN-REV-2026-0941 has been issued. Please review and acknowledge.', 'ACTION_REQUIRED', 'UNREAD'), " +
                    "(3, 3, 'Request Under Committee Review', 'Your transfer request (REQ-2) has been forwarded to the Transfer Committee for review.', 'INFO', 'READ'), " +
                    "(4, 1, 'New Transfer Request Submitted', 'New request #4 submitted by Officer.', 'INFO', 'UNREAD');");

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "log_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER REFERENCES users(user_id) ON DELETE SET NULL, " +
                    "role TEXT, " +
                    "action TEXT NOT NULL, " +
                    "entity TEXT NOT NULL, " +
                    "entity_id INTEGER, " +
                    "description TEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP);");

                stmt.executeUpdate("INSERT OR IGNORE INTO audit_logs (log_id, user_id, role, action, entity, entity_id, description) VALUES " +
                    "(1, 2, 'CIVIL_SERVICE_OFFICER', 'SUBMIT_REQUEST', 'transfer_requests', 1, 'Officer Arun Kumar submitted a transfer request for Chennai.'), " +
                    "(2, 1, 'CADRE_ADMINISTRATOR', 'VERIFY_DOCUMENT', 'documents', 1, 'Administrator verified medical document for Request #1.'), " +
                    "(3, 7, 'TRANSFER_COMMITTEE_MEMBER', 'APPROVE_REQUEST', 'transfer_requests', 1, 'Transfer Committee Member approved Request #1.'), " +
                    "(4, 1, 'CADRE_ADMINISTRATOR', 'GENERATE_ORDER', 'transfer_orders', 1, 'Transfer Order GO-TN-REV-2026-0941 generated for Officer Arun Kumar.');");

                System.out.println("SQLite Database initialized successfully!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logAudit(Connection conn, Integer userId, String role, String action, String entity, Integer entityId, String description) {
        boolean closeConn = false;
        try {
            if (conn == null || conn.isClosed()) {
                conn = getConnection();
                closeConn = true;
            }
            String sql = "INSERT INTO audit_logs (user_id, role, action, entity, entity_id, description) VALUES (?, ?, ?, ?, ?, ?)";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                if (userId != null && userId > 0) ps.setInt(1, userId); else ps.setNull(1, java.sql.Types.INTEGER);
                ps.setString(2, role != null ? role : "SYSTEM");
                ps.setString(3, action != null ? action : "UNKNOWN");
                ps.setString(4, entity != null ? entity : "SYSTEM");
                if (entityId != null && entityId > 0) ps.setInt(5, entityId); else ps.setNull(5, java.sql.Types.INTEGER);
                ps.setString(6, description != null ? description : "");
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (closeConn && conn != null) {
                try { conn.close(); } catch (Exception ignore) {}
            }
        }
    }
}



