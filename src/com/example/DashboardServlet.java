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

@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {

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
        String role = (String) session.getAttribute("role");
        Integer officerId = (Integer) session.getAttribute("officer_id");

        try (Connection conn = DBConnection.getConnection()) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"status\":\"success\",\"data\":{");

            if ("CIVIL_SERVICE_OFFICER".equals(role)) {
                // Officer dashboard metrics
                String offSql = "SELECT current_posting, current_district, cadre, years_in_current_posting FROM officers WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(offSql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            sb.append("\"current_posting\":\"").append(escapeJson(rs.getString("current_posting"))).append("\",");
                            sb.append("\"current_district\":\"").append(escapeJson(rs.getString("current_district"))).append("\",");
                            sb.append("\"cadre\":\"").append(escapeJson(rs.getString("cadre"))).append("\",");
                            sb.append("\"years_in_current_posting\":").append(rs.getInt("years_in_current_posting")).append(",");
                        }
                    }
                }

                if (officerId != null && officerId > 0) {
                    int totalReq = getCount(conn, "SELECT COUNT(*) FROM transfer_requests WHERE officer_id = ?", officerId);
                    int pendingReq = getCount(conn, "SELECT COUNT(*) FROM transfer_requests WHERE officer_id = ? AND status IN ('SUBMITTED','VERIFIED','FORWARDED','COMMITTEE_REVIEW')", officerId);
                    int approvedReq = getCount(conn, "SELECT COUNT(*) FROM transfer_requests WHERE officer_id = ? AND status = 'APPROVED'", officerId);
                    int rejectedReq = getCount(conn, "SELECT COUNT(*) FROM transfer_requests WHERE officer_id = ? AND status = 'REJECTED'", officerId);

                    sb.append("\"total_requests\":").append(totalReq).append(",");
                    sb.append("\"pending_requests\":").append(pendingReq).append(",");
                    sb.append("\"approved_requests\":").append(approvedReq).append(",");
                    sb.append("\"rejected_requests\":").append(rejectedReq).append(",");
                }

                int unreadNotif = getCount(conn, "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND read_status = 'UNREAD'", userId);
                sb.append("\"unread_notifications\":").append(unreadNotif);

                // Latest order check
                if (officerId != null && officerId > 0) {
                    String ordSql = "SELECT order_id, order_number, new_location FROM transfer_orders WHERE officer_id = ? AND status = 'GENERATED' ORDER BY order_id DESC LIMIT 1";
                    try (PreparedStatement ps = conn.prepareStatement(ordSql)) {
                        ps.setInt(1, officerId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                sb.append(",\"latest_order\":{");
                                sb.append("\"order_id\":").append(rs.getInt("order_id")).append(",");
                                sb.append("\"order_number\":\"").append(escapeJson(rs.getString("order_number"))).append("\",");
                                sb.append("\"new_location\":\"").append(escapeJson(rs.getString("new_location"))).append("\"");
                                sb.append("}");
                            }
                        }
                    }
                }
            } else if ("CADRE_ADMINISTRATOR".equals(role)) {
                // Admin dashboard metrics
                int totalOff = getCount(conn, "SELECT COUNT(*) FROM officers", -1);
                int totalReq = getCount(conn, "SELECT COUNT(*) FROM transfer_requests", -1);
                int pendingReq = getCount(conn, "SELECT COUNT(*) FROM transfer_requests WHERE status = 'SUBMITTED'", -1);
                int commReq = getCount(conn, "SELECT COUNT(*) FROM transfer_requests WHERE status = 'COMMITTEE_REVIEW'", -1);
                int appReq = getCount(conn, "SELECT COUNT(*) FROM transfer_requests WHERE status = 'APPROVED'", -1);
                int vac = getCount(conn, "SELECT COALESCE(SUM(available_positions), 0) FROM vacancies WHERE status = 'AVAILABLE'", -1);

                sb.append("\"total_officers\":").append(totalOff).append(",");
                sb.append("\"total_requests\":").append(totalReq).append(",");
                sb.append("\"pending_requests\":").append(pendingReq).append(",");
                sb.append("\"committee_requests\":").append(commReq).append(",");
                sb.append("\"approved_requests\":").append(appReq).append(",");
                sb.append("\"available_vacancies\":").append(vac);
            } else if ("TRANSFER_COMMITTEE_MEMBER".equals(role)) {
                // Committee dashboard metrics
                int awaiting = getCount(conn, "SELECT COUNT(*) FROM transfer_requests WHERE status = 'COMMITTEE_REVIEW'", -1);
                int highP = getCount(conn, "SELECT COUNT(*) FROM transfer_requests WHERE status = 'COMMITTEE_REVIEW' AND priority = 'HIGH'", -1);
                int medP = getCount(conn, "SELECT COUNT(*) FROM transfer_requests WHERE status = 'COMMITTEE_REVIEW' AND priority = 'MEDIUM'", -1);
                int approved = getCount(conn, "SELECT COUNT(*) FROM reviews WHERE recommendation = 'APPROVED'", -1);
                int rejected = getCount(conn, "SELECT COUNT(*) FROM reviews WHERE recommendation = 'REJECTED'", -1);

                sb.append("\"awaiting_review\":").append(awaiting).append(",");
                sb.append("\"high_priority\":").append(highP).append(",");
                sb.append("\"medium_priority\":").append(medP).append(",");
                sb.append("\"approved_requests\":").append(approved).append(",");
                sb.append("\"rejected_requests\":").append(rejected);
            }

            sb.append("}}");
            out.print(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
        out.flush();
    }

    private int getCount(Connection conn, String sql, int param) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != -1) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
