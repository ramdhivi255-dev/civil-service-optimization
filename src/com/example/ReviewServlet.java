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

@WebServlet("/api/review")
public class ReviewServlet extends HttpServlet {

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

        String reqIdStr = request.getParameter("request_id");
        if (reqIdStr == null || reqIdStr.isEmpty()) {
            out.print("{\"status\":\"error\",\"message\":\"request_id parameter required\"}");
            return;
        }

        int reqId = Integer.parseInt(reqIdStr);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT r.*, u.name AS reviewer_name FROM reviews r JOIN users u ON r.reviewer_id = u.user_id WHERE r.request_id = ? ORDER BY r.review_id DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, reqId);
                try (ResultSet rs = ps.executeQuery()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("{\"status\":\"success\",\"reviews\":[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        sb.append("{");
                        sb.append("\"review_id\":").append(rs.getInt("review_id")).append(",");
                        sb.append("\"request_id\":").append(rs.getInt("request_id")).append(",");
                        sb.append("\"reviewer_name\":\"").append(escapeJson(rs.getString("reviewer_name"))).append("\",");
                        sb.append("\"recommendation\":\"").append(escapeJson(rs.getString("recommendation"))).append("\",");
                        sb.append("\"remarks\":\"").append(escapeJson(rs.getString("remarks"))).append("\",");
                        sb.append("\"review_date\":\"").append(rs.getString("review_date") != null ? rs.getString("review_date") : "").append("\"");
                        sb.append("}");
                        first = false;
                    }
                    sb.append("]}");
                    out.print(sb.toString());
                }
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

        int reviewerId = (Integer) session.getAttribute("user_id");
        String reqIdStr = request.getParameter("request_id");
        String rec = request.getParameter("recommendation");
        String remarks = request.getParameter("remarks");

        if (reqIdStr == null || rec == null) {
            out.print("{\"status\":\"error\",\"message\":\"Missing required fields\"}");
            return;
        }

        int reqId = Integer.parseInt(reqIdStr);
        String finalStatus = "APPROVED".equalsIgnoreCase(rec) ? "APPROVED" : "REJECTED";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert review
                String sqlRev = "INSERT INTO reviews (request_id, reviewer_id, recommendation, remarks) VALUES (?, ?, ?, ?)";
                try (PreparedStatement psRev = conn.prepareStatement(sqlRev)) {
                    psRev.setInt(1, reqId);
                    psRev.setInt(2, reviewerId);
                    psRev.setString(3, rec);
                    psRev.setString(4, remarks != null ? remarks : "");
                    psRev.executeUpdate();
                }

                // Update request status
                String sqlReq = "UPDATE transfer_requests SET status = ?, remarks = ? WHERE request_id = ?";
                try (PreparedStatement psReq = conn.prepareStatement(sqlReq)) {
                    psReq.setString(1, finalStatus);
                    psReq.setString(2, remarks != null ? remarks : "");
                    psReq.setInt(3, reqId);
                    psReq.executeUpdate();
                }

                // Notify officer
                String notifSql = "INSERT INTO notifications (user_id, title, message, notification_type) " +
                                 "SELECT o.user_id, 'Committee Decision Result', 'Transfer Request #" + reqId + " has been " + finalStatus + " by the Transfer Committee.', 'COMMITTEE_DECISION' " +
                                 "FROM transfer_requests tr JOIN officers o ON tr.officer_id = o.officer_id WHERE tr.request_id = ?";
                try (PreparedStatement psN = conn.prepareStatement(notifSql)) {
                    psN.setInt(1, reqId);
                    psN.executeUpdate();
                }

                conn.commit();
                out.print("{\"status\":\"success\",\"message\":\"Committee decision submitted successfully\"}");
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
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
