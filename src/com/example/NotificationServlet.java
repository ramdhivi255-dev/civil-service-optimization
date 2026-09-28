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

@WebServlet({"/api/notification", "/notification"})
public class NotificationServlet extends HttpServlet {

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

        int userId = (Integer) session.getAttribute("user_id");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM notifications WHERE user_id = ? ORDER BY notification_id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                sb.append("{\"status\":\"success\",\"notifications\":[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{");
                    sb.append("\"notification_id\":").append(rs.getInt("notification_id")).append(",");
                    sb.append("\"title\":\"").append(escapeJson(rs.getString("title"))).append("\",");
                    sb.append("\"message\":\"").append(escapeJson(rs.getString("message"))).append("\",");
                    sb.append("\"notification_type\":\"").append(escapeJson(rs.getString("notification_type"))).append("\",");
                    sb.append("\"read_status\":\"").append(escapeJson(rs.getString("read_status"))).append("\",");
                    sb.append("\"created_at\":\"").append(rs.getString("created_at") != null ? rs.getString("created_at") : "").append("\"");
                    sb.append("}");
                    first = false;
                }
                sb.append("]}");
                out.print(sb.toString());
            }
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

        int userId = (Integer) session.getAttribute("user_id");
        String action = request.getParameter("action");

        try (Connection conn = DBConnection.getConnection()) {
            if ("mark_read".equals(action)) {
                String notifIdStr = request.getParameter("notification_id");
                if (notifIdStr != null && !notifIdStr.isEmpty()) {
                    String sql = "UPDATE notifications SET read_status = 'READ' WHERE notification_id = ? AND user_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, Integer.parseInt(notifIdStr));
                        ps.setInt(2, userId);
                        ps.executeUpdate();
                    }
                }
                out.print("{\"status\":\"success\",\"message\":\"Notification marked as read\"}");
            } else if ("mark_all_read".equals(action)) {
                String sql = "UPDATE notifications SET read_status = 'READ' WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }
                out.print("{\"status\":\"success\",\"message\":\"All notifications marked as read\"}");
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
