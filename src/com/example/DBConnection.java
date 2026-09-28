package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static String getEnv(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.trim().isEmpty()) ? val.trim() : defaultValue;
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        String host = getEnv("DB_HOST", "localhost");
        String port = getEnv("DB_PORT", "3306");
        String dbName = getEnv("DB_NAME", "civil_services_transfer_system");
        String user = getEnv("DB_USER", "root");
        String password = getEnv("DB_PASSWORD", "root");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, user, password);
    }
}

