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

@WebServlet({"/api/optimization", "/optimization"})
public class OptimizationServlet extends HttpServlet {

    public static class OptimizationResult {
        public int totalScore;
        public String priority;
        public String eligibilityStatus;
        public int tenureScore;
        public int vacancyScore;
        public int reasonScore;
        public int preferenceScore;
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
        String reqIdStr = request.getParameter("request_id");

        if (reqIdStr == null || reqIdStr.isEmpty()) {
            out.print("{\"status\":\"error\",\"message\":\"request_id parameter is required\"}");
            return;
        }

        int requestId = Integer.parseInt(reqIdStr);

        try (Connection conn = DBConnection.getConnection()) {
            if ("verify_and_score".equals(action)) {
                OptimizationResult res = calculateScore(conn, requestId);
                if (res != null) {
                    // Update database
                    String updateSql = "UPDATE transfer_requests SET optimization_score = ?, priority = ?, eligibility_status = ? WHERE request_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setInt(1, res.totalScore);
                        ps.setString(2, res.priority);
                        ps.setString(3, res.eligibilityStatus);
                        ps.setInt(4, requestId);
                        ps.executeUpdate();
                    }

                    out.print("{\"status\":\"success\",\"result\":{" +
                            "\"total_score\":" + res.totalScore + "," +
                            "\"priority\":\"" + res.priority + "\"," +
                            "\"eligibility_status\":\"" + res.eligibilityStatus + "\"," +
                            "\"breakdown\":{" +
                            "\"tenure_score\":" + res.tenureScore + "," +
                            "\"vacancy_score\":" + res.vacancyScore + "," +
                            "\"reason_score\":" + res.reasonScore + "," +
                            "\"preference_score\":" + res.preferenceScore +
                            "}" +
                            "}}");
                } else {
                    out.print("{\"status\":\"error\",\"message\":\"Transfer request not found\"}");
                }
            } else if ("forward_committee".equals(action)) {
                String sql = "UPDATE transfer_requests SET status = 'COMMITTEE_REVIEW', eligibility_status = 'ELIGIBLE' WHERE request_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, requestId);
                    ps.executeUpdate();
                }

                // Send notification to committee
                String notifSql = "INSERT INTO notifications (user_id, title, message, notification_type) " +
                                 "SELECT user_id, 'New Transfer Request for Review', 'Request #' || ? || ' is awaiting committee review.', 'SYSTEM' " +
                                 "FROM users WHERE role = 'TRANSFER_COMMITTEE_MEMBER'";
                try (PreparedStatement psNotif = conn.prepareStatement(notifSql)) {
                    psNotif.setInt(1, requestId);
                    psNotif.executeUpdate();
                } catch (Exception ignore) {}

                out.print("{\"status\":\"success\",\"message\":\"Forwarded to committee successfully\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
        out.flush();
    }

    public static OptimizationResult calculateScore(Connection conn, int requestId) throws Exception {
        String sql = "SELECT tr.*, o.years_in_current_posting, o.current_posting, o.current_district, o.cadre " +
                     "FROM transfer_requests tr JOIN officers o ON tr.officer_id = o.officer_id WHERE tr.request_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    OptimizationResult result = new OptimizationResult();
                    int years = rs.getInt("years_in_current_posting");
                    String pref1 = rs.getString("preferred_location_1");
                    String reason = rs.getString("reason");

                    // 1. Tenure Score (0 to 35)
                    if (years >= 5) result.tenureScore = 35;
                    else if (years == 4) result.tenureScore = 30;
                    else if (years == 3) result.tenureScore = 25;
                    else if (years == 2) result.tenureScore = 15;
                    else result.tenureScore = 5;

                    // 2. Vacancy Score (0 to 30) based on availability in preferred location
                    int availableVacancies = 0;
                    if (pref1 != null && !pref1.isEmpty()) {
                        String vacSql = "SELECT SUM(available_positions) FROM vacancies WHERE (location LIKE ? OR district LIKE ?) AND status = 'AVAILABLE'";
                        try (PreparedStatement psV = conn.prepareStatement(vacSql)) {
                            String pat = "%" + pref1 + "%";
                            psV.setString(1, pat);
                            psV.setString(2, pat);
                            try (ResultSet rsV = psV.executeQuery()) {
                                if (rsV.next()) availableVacancies = rsV.getInt(1);
                            }
                        }
                    }
                    if (availableVacancies >= 3) result.vacancyScore = 30;
                    else if (availableVacancies == 2) result.vacancyScore = 24;
                    else if (availableVacancies == 1) result.vacancyScore = 18;
                    else result.vacancyScore = 5;

                    // 3. Reason Priority Score (0 to 20)
                    String lReason = reason != null ? reason.toLowerCase() : "";
                    if (lReason.contains("medical") || lReason.contains("health") || lReason.contains("spouse")) {
                        result.reasonScore = 20;
                    } else if (lReason.contains("family") || lReason.contains("education") || lReason.contains("children")) {
                        result.reasonScore = 15;
                    } else {
                        result.reasonScore = 10;
                    }

                    // 4. Preference Score (0 to 15)
                    result.preferenceScore = 15;

                    // Total Calculation (0 - 100)
                    result.totalScore = Math.min(100, result.tenureScore + result.vacancyScore + result.reasonScore + result.preferenceScore);

                    if (result.totalScore >= 75) {
                        result.priority = "HIGH";
                        result.eligibilityStatus = "ELIGIBLE";
                    } else if (result.totalScore >= 50) {
                        result.priority = "MEDIUM";
                        result.eligibilityStatus = "ELIGIBLE";
                    } else {
                        result.priority = "LOW";
                        result.eligibilityStatus = "PENDING_VERIFICATION";
                    }

                    return result;
                }
            }
        }
        return null;
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
