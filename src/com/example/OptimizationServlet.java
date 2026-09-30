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

@WebServlet("/api/optimization")
public class OptimizationServlet extends HttpServlet {

    public static class OptimizationResult {
        public int totalScore;
        public String priority;
        public String eligibilityStatus;
        public int eligibilityScore; // Max 25
        public int tenureScore;      // Max 20
        public int preferenceScore;  // Max 20
        public int vacancyScore;     // Max 15
        public int reasonScore;      // Max 10 (Admin Priority)
        public int historyScore;     // Max 10 (Transfer History / Stability)
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
        int userId = (Integer) session.getAttribute("user_id");
        String role = (String) session.getAttribute("role");

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

                    DBConnection.logAudit(conn, userId, role, "VERIFY_AND_SCORE", "transfer_requests", requestId, "Calculated optimization score (" + res.totalScore + "/100) and priority (" + res.priority + ") for Request #" + requestId);

                    out.print("{\"status\":\"success\",\"result\":{" +
                            "\"total_score\":" + res.totalScore + "," +
                            "\"priority\":\"" + res.priority + "\"," +
                            "\"eligibility_status\":\"" + res.eligibilityStatus + "\"," +
                            "\"breakdown\":{" +
                            "\"eligibility_score\":" + res.eligibilityScore + "," +
                            "\"tenure_score\":" + res.tenureScore + "," +
                            "\"preference_score\":" + res.preferenceScore + "," +
                            "\"vacancy_score\":" + res.vacancyScore + "," +
                            "\"reason_score\":" + res.reasonScore + "," +
                            "\"history_score\":" + res.historyScore +
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

                DBConnection.logAudit(conn, userId, role, "FORWARD_TO_COMMITTEE", "transfer_requests", requestId, "Forwarded Request #" + requestId + " to Transfer Committee for review");

                // Send notification to committee members
                String notifSql = "INSERT INTO notifications (user_id, title, message, notification_type) " +
                                 "SELECT user_id, 'New Transfer Request for Review', CONCAT('Request #', ?, ' is awaiting committee review.'), 'SYSTEM' " +
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
                    String pref2 = rs.getString("preferred_location_2");
                    String reason = rs.getString("reason");

                    // 1. Core Eligibility Baseline (25 pts)
                    result.eligibilityScore = 25;

                    // 2. Years in Current Posting (20 pts)
                    if (years >= 5) result.tenureScore = 20;
                    else if (years == 4) result.tenureScore = 16;
                    else if (years == 3) result.tenureScore = 12;
                    else if (years == 2) result.tenureScore = 8;
                    else result.tenureScore = 4;

                    // 3. Officer Preference Match (20 pts)
                    if (pref1 != null && !pref1.trim().isEmpty()) {
                        result.preferenceScore = 20;
                    } else if (pref2 != null && !pref2.trim().isEmpty()) {
                        result.preferenceScore = 15;
                    } else {
                        result.preferenceScore = 10;
                    }

                    // 4. Vacancy Availability Score (15 pts)
                    int availableVacancies = 0;
                    if (pref1 != null && !pref1.isEmpty()) {
                        String vacSql = "SELECT COALESCE(SUM(available_positions), 0) FROM vacancies WHERE (location LIKE ? OR district LIKE ?) AND status = 'AVAILABLE'";
                        try (PreparedStatement psV = conn.prepareStatement(vacSql)) {
                            String pat = "%" + pref1.trim() + "%";
                            psV.setString(1, pat);
                            psV.setString(2, pat);
                            try (ResultSet rsV = psV.executeQuery()) {
                                if (rsV.next()) availableVacancies = rsV.getInt(1);
                            }
                        }
                    }
                    if (availableVacancies >= 3) result.vacancyScore = 15;
                    else if (availableVacancies == 2) result.vacancyScore = 12;
                    else if (availableVacancies == 1) result.vacancyScore = 9;
                    else result.vacancyScore = 3;

                    // 5. Administrative Priority / Reason Score (10 pts)
                    String lReason = reason != null ? reason.toLowerCase() : "";
                    if (lReason.contains("medical") || lReason.contains("health") || lReason.contains("spouse")) {
                        result.reasonScore = 10;
                    } else if (lReason.contains("family") || lReason.contains("education") || lReason.contains("children")) {
                        result.reasonScore = 7;
                    } else {
                        result.reasonScore = 5;
                    }

                    // 6. Transfer History / Stability Score (10 pts)
                    if (years >= 3) {
                        result.historyScore = 10;
                    } else {
                        result.historyScore = 5;
                    }

                    // Total Calculation (0 - 100)
                    result.totalScore = Math.min(100, result.eligibilityScore + result.tenureScore + result.preferenceScore + result.vacancyScore + result.reasonScore + result.historyScore);

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
