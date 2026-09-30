package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
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
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        String host = getEnv("DB_HOST", "MYSQLHOST", "MYSQL_HOST", "localhost");
        String port = getEnv("DB_PORT", "MYSQLPORT", "MYSQL_PORT", "3306");
        String dbName = getEnv("DB_NAME", "MYSQLDATABASE", "MYSQL_DATABASE", "civil_services_transfer_system");
        String user = getEnv("DB_USER", "MYSQLUSER", "MYSQL_USER", "root");
        String password = getEnv("DB_PASSWORD", "MYSQLPASSWORD", "MYSQL_PASSWORD", "root");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, user, password);
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

