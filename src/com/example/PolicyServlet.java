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

@WebServlet({"/api/policy", "/policy"})
public class PolicyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            out.print("{\"status\":\"unauthenticated\"}");
            return;
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM transfer_policies ORDER BY policy_id ASC");
             ResultSet rs = ps.executeQuery()) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"status\":\"success\",\"policies\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                sb.append("{");
                sb.append("\"policy_id\":").append(rs.getInt("policy_id")).append(",");
                sb.append("\"policy_name\":\"").append(escapeJson(rs.getString("policy_name"))).append("\",");
                sb.append("\"description\":\"").append(escapeJson(rs.getString("description"))).append("\",");
                sb.append("\"minimum_service_years\":").append(rs.getInt("minimum_service_years")).append(",");
                sb.append("\"maximum_transfer_frequency\":").append(rs.getInt("maximum_transfer_frequency")).append(",");
                sb.append("\"priority_rules\":\"").append(escapeJson(rs.getString("priority_rules"))).append("\",");
                sb.append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\"");
                sb.append("}");
                first = false;
            }
            sb.append("]}");
            out.print(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            out.print("{\"status\":\"unauthenticated\"}");
            return;
        }

        String action = request.getParameter("action");

        try (Connection conn = DBConnection.getConnection()) {
            if ("add".equals(action) || "create".equals(action)) {
                String name = request.getParameter("policy_name");
                String desc = request.getParameter("description");
                String minYrs = request.getParameter("minimum_service_years");
                String maxFreq = request.getParameter("maximum_transfer_frequency");
                String rules = request.getParameter("priority_rules");

                String sql = "INSERT INTO transfer_policies (policy_name, description, minimum_service_years, maximum_transfer_frequency, priority_rules, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, desc);
                    ps.setInt(3, minYrs != null && !minYrs.isEmpty() ? Integer.parseInt(minYrs) : 3);
                    ps.setInt(4, maxFreq != null && !maxFreq.isEmpty() ? Integer.parseInt(maxFreq) : 1);
                    ps.setString(5, rules);
                    ps.executeUpdate();
                }
                out.print("{\"status\":\"success\",\"message\":\"Policy created successfully\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
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
