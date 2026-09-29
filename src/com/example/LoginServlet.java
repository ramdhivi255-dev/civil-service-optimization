package com.example;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet({"/api/login", "/api/auth/status"})
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user_id") != null) {
            int userId = (Integer) session.getAttribute("user_id");
            String username = (String) session.getAttribute("username");
            String role = (String) session.getAttribute("role");
            String name = (String) session.getAttribute("name");
            Integer officerId = (Integer) session.getAttribute("officer_id");

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"authenticated\",");
            json.append("\"user_id\":").append(userId).append(",");
            json.append("\"username\":\"").append(escapeJson(username)).append("\",");
            json.append("\"role\":\"").append(escapeJson(role)).append("\",");
            json.append("\"name\":\"").append(escapeJson(name != null ? name : username)).append("\"");
            if (officerId != null) {
                json.append(",\"officer_id\":").append(officerId);
            }
            json.append("}");
            out.print(json.toString());
        } else {
            out.print("{\"status\":\"unauthenticated\"}");
        }
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            out.print("{\"status\":\"error\",\"message\":\"Username and password are required\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT u.user_id, u.username, u.password, u.name, u.role, o.officer_id " +
                         "FROM users u LEFT JOIN officers o ON u.user_id = o.user_id " +
                         "WHERE u.username = ? AND u.status = 'ACTIVE'";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username.trim());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String dbPassword = rs.getString("password");
                        if (dbPassword.equals(password.trim())) {
                            int userId = rs.getInt("user_id");
                            String role = rs.getString("role");
                            String name = rs.getString("name");
                            int officerId = rs.getInt("officer_id");

                            HttpSession session = request.getSession(true);
                            session.setAttribute("user_id", userId);
                            session.setAttribute("username", username);
                            session.setAttribute("role", role);
                            session.setAttribute("name", name);
                            if (officerId > 0) {
                                session.setAttribute("officer_id", officerId);
                            }

                            String redirect = "officer/dashboard.html";
                            if ("CADRE_ADMINISTRATOR".equals(role)) {
                                redirect = "admin/dashboard.html";
                            } else if ("TRANSFER_COMMITTEE_MEMBER".equals(role)) {
                                redirect = "committee/dashboard.html";
                            }

                            out.print("{\"status\":\"success\",\"redirect\":\"" + redirect + "\",\"role\":\"" + role + "\"}");
                            return;
                        }
                    }
                }
            }
            out.print("{\"status\":\"error\",\"message\":\"Invalid username or password\"}");
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\",\"message\":\"Database error: " + escapeJson(e.getMessage()) + "\"}");
        }
        out.flush();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
