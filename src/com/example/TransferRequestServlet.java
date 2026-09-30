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
import java.sql.Statement;

@WebServlet("/api/transfer-request")
public class TransferRequestServlet extends HttpServlet {

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

        String action = request.getParameter("action");
        Integer officerId = (Integer) session.getAttribute("officer_id");

        try (Connection conn = DBConnection.getConnection()) {
            if ("my_requests".equals(action)) {
                if (officerId == null || officerId <= 0) {
                    out.print("{\"status\":\"error\",\"message\":\"Officer profile not linked\"}");
                    return;
                }
                String sql = "SELECT tr.*, o.name AS officer_name, o.employee_id, o.cadre FROM transfer_requests tr " +
                             "JOIN officers o ON tr.officer_id = o.officer_id WHERE tr.officer_id = ? ORDER BY tr.request_id DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, officerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        out.print("{\"status\":\"success\",\"requests\":" + buildRequestListJson(rs) + "}");
                    }
                }
            } else if ("list_all".equals(action)) {
                String search = request.getParameter("search");
                String status = request.getParameter("status");
                String priority = request.getParameter("priority");

                StringBuilder sql = new StringBuilder("SELECT tr.*, o.name AS officer_name, o.employee_id, o.cadre, o.current_posting, o.current_district, o.designation FROM transfer_requests tr JOIN officers o ON tr.officer_id = o.officer_id WHERE 1=1");

                if (search != null && !search.trim().isEmpty()) {
                    sql.append(" AND (o.name LIKE ? OR o.employee_id LIKE ? OR tr.preferred_location_1 LIKE ?)");
                }
                if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                    sql.append(" AND tr.status = ?");
                }
                if (priority != null && !priority.trim().isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
                    sql.append(" AND tr.priority = ?");
                }
                sql.append(" ORDER BY tr.request_id DESC");

                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    int pIndex = 1;
                    if (search != null && !search.trim().isEmpty()) {
                        String pat = "%" + search.trim() + "%";
                        ps.setString(pIndex++, pat);
                        ps.setString(pIndex++, pat);
                        ps.setString(pIndex++, pat);
                    }
                    if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                        ps.setString(pIndex++, status.trim());
                    }
                    if (priority != null && !priority.trim().isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
                        ps.setString(pIndex++, priority.trim());
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        out.print("{\"status\":\"success\",\"requests\":" + buildRequestListJson(rs) + "}");
                    }
                }
            } else if ("get_details".equals(action)) {
                String reqIdStr = request.getParameter("request_id");
                if (reqIdStr == null || reqIdStr.isEmpty()) {
                    out.print("{\"status\":\"error\",\"message\":\"request_id is required\"}");
                    return;
                }
                int reqId = Integer.parseInt(reqIdStr);
                String sql = "SELECT tr.*, o.name AS officer_name, o.employee_id, o.cadre, o.email AS officer_email, o.phone AS officer_phone, " +
                             "o.designation, o.department, o.current_posting, o.current_district, o.years_in_current_posting " +
                             "FROM transfer_requests tr JOIN officers o ON tr.officer_id = o.officer_id WHERE tr.request_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, reqId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            out.print("{\"status\":\"success\",\"request\":" + buildSingleRequestJson(rs) + "}");
                        } else {
                            out.print("{\"status\":\"error\",\"message\":\"Request not found\"}");
                        }
                    }
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

        String action = request.getParameter("action");
        Integer officerId = (Integer) session.getAttribute("officer_id");
        Integer userId = (Integer) session.getAttribute("user_id");

        try (Connection conn = DBConnection.getConnection()) {
            if ("create".equals(action)) {
                if (officerId == null || officerId <= 0) {
                    if (userId != null && userId > 0) {
                        String findSql = "SELECT officer_id FROM officers WHERE user_id = ?";
                        try (PreparedStatement psF = conn.prepareStatement(findSql)) {
                            psF.setInt(1, userId);
                            try (ResultSet rsF = psF.executeQuery()) {
                                if (rsF.next()) {
                                    officerId = rsF.getInt("officer_id");
                                    session.setAttribute("officer_id", officerId);
                                }
                            }
                        }
                    }
                }

                if (officerId == null || officerId <= 0) {
                    // Fallback default for demo officer profile
                    officerId = 1;
                }
                String reason = request.getParameter("reason");
                String pref1 = request.getParameter("preferred_location_1");
                String pref2 = request.getParameter("preferred_location_2");
                String pref3 = request.getParameter("preferred_location_3");

                String sql = "INSERT INTO transfer_requests (officer_id, reason, preferred_location_1, preferred_location_2, preferred_location_3, status, eligibility_status) VALUES (?, ?, ?, ?, ?, 'SUBMITTED', 'PENDING_VERIFICATION')";
                int generatedId = 0;
                try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, officerId);
                    ps.setString(2, reason);
                    ps.setString(3, pref1);
                    ps.setString(4, pref2);
                    ps.setString(5, pref3);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) generatedId = rs.getInt(1);
                    }
                }

                // Initial scoring
                try {
                    OptimizationServlet.OptimizationResult res = OptimizationServlet.calculateScore(conn, generatedId);
                    if (res != null) {
                        String scoreSql = "UPDATE transfer_requests SET optimization_score = ?, priority = ?, eligibility_status = ? WHERE request_id = ?";
                        try (PreparedStatement psScore = conn.prepareStatement(scoreSql)) {
                            psScore.setInt(1, res.totalScore);
                            psScore.setString(2, res.priority);
                            psScore.setString(3, res.eligibilityStatus);
                            psScore.setInt(4, generatedId);
                            psScore.executeUpdate();
                        }
                    }
                } catch (Exception ignore) {}

                int currentUserId = (userId != null && userId > 0) ? userId : 1;
                DBConnection.logAudit(conn, currentUserId, (String) session.getAttribute("role"), "SUBMIT_REQUEST", "transfer_requests", generatedId, "Submitted transfer request for " + pref1);

                // Notify Officer
                String notifUser = "INSERT INTO notifications (user_id, title, message, notification_type) VALUES (?, 'Transfer Request Submitted', 'Your transfer request (#" + generatedId + ") for " + escapeJson(pref1) + " has been submitted successfully.', 'INFO')";
                try (PreparedStatement psN = conn.prepareStatement(notifUser)) {
                    psN.setInt(1, currentUserId);
                    psN.executeUpdate();
                } catch (Exception ignore) {}

                // Notify Admins
                String notifAdmin = "INSERT INTO notifications (user_id, title, message, notification_type) SELECT user_id, 'New Transfer Request', 'New transfer request (#" + generatedId + ") submitted by officer.', 'SYSTEM' FROM users WHERE role = 'CADRE_ADMINISTRATOR'";
                try (PreparedStatement psNA = conn.prepareStatement(notifAdmin)) {
                    psNA.executeUpdate();
                } catch (Exception ignore) {}

                out.print("{\"status\":\"success\",\"message\":\"Transfer request submitted successfully\",\"request_id\":" + generatedId + "}");
            } else if ("update_status".equals(action) || "reject".equals(action)) {
                String reqIdStr = request.getParameter("request_id");
                String status = request.getParameter("status");
                String remarks = request.getParameter("remarks");
                if (status == null || status.isEmpty()) status = "REJECTED";

                int reqId = Integer.parseInt(reqIdStr);
                String sql = "UPDATE transfer_requests SET status = ?, remarks = ? WHERE request_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, status);
                    ps.setString(2, remarks != null ? remarks : "");
                    ps.setInt(3, reqId);
                    ps.executeUpdate();
                }

                int currentUserId = (userId != null && userId > 0) ? userId : 1;
                DBConnection.logAudit(conn, currentUserId, (String) session.getAttribute("role"), "REJECT".equals(status) ? "REJECT_REQUEST" : "UPDATE_STATUS", "transfer_requests", reqId, "Transfer request #" + reqId + " status updated to " + status);

                // Notify officer
                String notifSql = "INSERT INTO notifications (user_id, title, message, notification_type) " +
                                 "SELECT o.user_id, 'Transfer Request Update', 'Your transfer request (#" + reqId + ") status has been updated to: " + status + "', 'STATUS_CHANGE' " +
                                 "FROM transfer_requests tr JOIN officers o ON tr.officer_id = o.officer_id WHERE tr.request_id = ?";
                try (PreparedStatement psN = conn.prepareStatement(notifSql)) {
                    psN.setInt(1, reqId);
                    psN.executeUpdate();
                } catch (Exception ignore) {}

                out.print("{\"status\":\"success\",\"message\":\"Request status updated to " + status + "\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
        out.flush();
    }

    private String buildRequestListJson(ResultSet rs) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        while (rs.next()) {
            if (!first) sb.append(",");
            sb.append(buildSingleRequestJson(rs));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildSingleRequestJson(ResultSet rs) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"request_id\":").append(rs.getInt("request_id")).append(",");
        sb.append("\"officer_id\":").append(rs.getInt("officer_id")).append(",");
        try { sb.append("\"officer_name\":\"").append(escapeJson(rs.getString("officer_name"))).append("\","); } catch (Exception e) {}
        try { sb.append("\"employee_id\":\"").append(escapeJson(rs.getString("employee_id"))).append("\","); } catch (Exception e) {}
        try { sb.append("\"cadre\":\"").append(escapeJson(rs.getString("cadre"))).append("\","); } catch (Exception e) {}
        try { sb.append("\"designation\":\"").append(escapeJson(rs.getString("designation"))).append("\","); } catch (Exception e) {}
        try { sb.append("\"department\":\"").append(escapeJson(rs.getString("department"))).append("\","); } catch (Exception e) {}
        try { sb.append("\"current_posting\":\"").append(escapeJson(rs.getString("current_posting"))).append("\","); } catch (Exception e) {}
        try { sb.append("\"current_district\":\"").append(escapeJson(rs.getString("current_district"))).append("\","); } catch (Exception e) {}
        try { sb.append("\"years_in_current_posting\":").append(rs.getInt("years_in_current_posting")).append(","); } catch (Exception e) {}
        sb.append("\"reason\":\"").append(escapeJson(rs.getString("reason"))).append("\",");
        sb.append("\"preferred_location_1\":\"").append(escapeJson(rs.getString("preferred_location_1"))).append("\",");
        sb.append("\"preferred_location_2\":\"").append(escapeJson(rs.getString("preferred_location_2"))).append("\",");
        sb.append("\"preferred_location_3\":\"").append(escapeJson(rs.getString("preferred_location_3"))).append("\",");
        sb.append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\",");
        sb.append("\"eligibility_status\":\"").append(escapeJson(rs.getString("eligibility_status"))).append("\",");
        sb.append("\"optimization_score\":").append(rs.getInt("optimization_score")).append(",");
        sb.append("\"priority\":\"").append(escapeJson(rs.getString("priority"))).append("\",");
        sb.append("\"remarks\":\"").append(escapeJson(rs.getString("remarks"))).append("\",");
        sb.append("\"created_at\":\"").append(rs.getString("created_at") != null ? rs.getString("created_at") : "").append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
