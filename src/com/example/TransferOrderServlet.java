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

@WebServlet("/api/transfer-order")
public class TransferOrderServlet extends HttpServlet {

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

        String role = (String) session.getAttribute("role");
        Integer officerId = (Integer) session.getAttribute("officer_id");

        try (Connection conn = DBConnection.getConnection()) {
            StringBuilder sql = new StringBuilder("SELECT tor.*, o.name AS officer_name, o.employee_id, o.designation FROM transfer_orders tor JOIN officers o ON tor.officer_id = o.officer_id WHERE 1=1");

            if ("CIVIL_SERVICE_OFFICER".equals(role)) {
                if (officerId == null || officerId <= 0) {
                    out.print("{\"status\":\"error\",\"message\":\"Officer record not found\"}");
                    return;
                }
                sql.append(" AND tor.officer_id = ").append(officerId);
            }
            sql.append(" ORDER BY tor.order_id DESC");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString());
                 ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                sb.append("{\"status\":\"success\",\"orders\":[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{");
                    sb.append("\"order_id\":").append(rs.getInt("order_id")).append(",");
                    sb.append("\"request_id\":").append(rs.getInt("request_id")).append(",");
                    sb.append("\"officer_id\":").append(rs.getInt("officer_id")).append(",");
                    sb.append("\"order_number\":\"").append(escapeJson(rs.getString("order_number"))).append("\",");
                    sb.append("\"officer_name\":\"").append(escapeJson(rs.getString("officer_name"))).append("\",");
                    sb.append("\"employee_id\":\"").append(escapeJson(rs.getString("employee_id"))).append("\",");
                    sb.append("\"designation\":\"").append(escapeJson(rs.getString("designation"))).append("\",");
                    sb.append("\"old_location\":\"").append(escapeJson(rs.getString("old_location"))).append("\",");
                    sb.append("\"new_location\":\"").append(escapeJson(rs.getString("new_location"))).append("\",");
                    sb.append("\"order_date\":\"").append(rs.getString("order_date") != null ? rs.getString("order_date") : "").append("\",");
                    sb.append("\"joining_date\":\"").append(rs.getString("joining_date") != null ? rs.getString("joining_date") : "").append("\",");
                    sb.append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\"");
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

        String action = request.getParameter("action");

        try (Connection conn = DBConnection.getConnection()) {
            if ("generate".equals(action) || "create".equals(action)) {
                String reqIdStr = request.getParameter("request_id");
                String joiningDate = request.getParameter("joining_date");
                String newLocation = request.getParameter("new_location");

                if (reqIdStr == null || reqIdStr.isEmpty()) {
                    out.print("{\"status\":\"error\",\"message\":\"request_id is required\"}");
                    return;
                }

                int reqId = Integer.parseInt(reqIdStr);

                // Fetch details from request
                String reqSql = "SELECT tr.officer_id, tr.preferred_location_1, o.current_posting FROM transfer_requests tr JOIN officers o ON tr.officer_id = o.officer_id WHERE tr.request_id = ?";
                int officerId = 0;
                String oldLoc = "";
                String targetLoc = newLocation;

                try (PreparedStatement ps = conn.prepareStatement(reqSql)) {
                    ps.setInt(1, reqId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            officerId = rs.getInt("officer_id");
                            oldLoc = rs.getString("current_posting");
                            if (targetLoc == null || targetLoc.trim().isEmpty()) {
                                targetLoc = rs.getString("preferred_location_1");
                            }
                        } else {
                            out.print("{\"status\":\"error\",\"message\":\"Request not found\"}");
                            return;
                        }
                    }
                }

                if (joiningDate == null || joiningDate.trim().isEmpty()) {
                    joiningDate = "2026-10-15";
                }

                String orderNum = "ORD-2026-" + System.currentTimeMillis() % 10000;

                conn.setAutoCommit(false);
                try {
                    String sqlOrd = "INSERT INTO transfer_orders (request_id, officer_id, order_number, old_location, new_location, joining_date, status) VALUES (?, ?, ?, ?, ?, ?, 'GENERATED')";
                    try (PreparedStatement psO = conn.prepareStatement(sqlOrd)) {
                        psO.setInt(1, reqId);
                        psO.setInt(2, officerId);
                        psO.setString(3, orderNum);
                        psO.setString(4, oldLoc);
                        psO.setString(5, targetLoc);
                        psO.setString(6, joiningDate);
                        psO.executeUpdate();
                    }

                    // Update request status
                    String sqlReq = "UPDATE transfer_requests SET status = 'ORDER_GENERATED' WHERE request_id = ?";
                    try (PreparedStatement psR = conn.prepareStatement(sqlReq)) {
                        psR.setInt(1, reqId);
                        psR.executeUpdate();
                    }

                    // Notify officer
                    String sqlNotif = "INSERT INTO notifications (user_id, title, message, notification_type) " +
                                     "SELECT user_id, 'Transfer Order Issued', 'Official Order #" + orderNum + " has been generated for your transfer to " + escapeJson(targetLoc) + "', 'ORDER_ISSUED' " +
                                     "FROM officers WHERE officer_id = ?";
                    try (PreparedStatement psN = conn.prepareStatement(sqlNotif)) {
                        psN.setInt(1, officerId);
                        psN.executeUpdate();
                    }

                    conn.commit();
                    out.print("{\"status\":\"success\",\"message\":\"Transfer Order generated successfully\",\"order_number\":\"" + orderNum + "\"}");
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            } else if ("acknowledge".equals(action)) {
                String orderIdStr = request.getParameter("order_id");
                if (orderIdStr == null || orderIdStr.isEmpty()) {
                    out.print("{\"status\":\"error\",\"message\":\"order_id is required\"}");
                    return;
                }
                int orderId = Integer.parseInt(orderIdStr);

                String sql = "UPDATE transfer_orders SET status = 'ACKNOWLEDGED' WHERE order_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, orderId);
                    ps.executeUpdate();
                }
                out.print("{\"status\":\"success\",\"message\":\"Transfer order acknowledged\"}");
            } else if ("confirm_joining".equals(action)) {
                String orderIdStr = request.getParameter("order_id");
                if (orderIdStr == null || orderIdStr.isEmpty()) {
                    out.print("{\"status\":\"error\",\"message\":\"order_id is required\"}");
                    return;
                }
                int orderId = Integer.parseInt(orderIdStr);

                conn.setAutoCommit(false);
                try {
                    String sqlOrd = "UPDATE transfer_orders SET status = 'JOINED' WHERE order_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlOrd)) {
                        ps.setInt(1, orderId);
                        ps.executeUpdate();
                    }

                    // Fetch new location and officer_id
                    String fetchSql = "SELECT officer_id, new_location FROM transfer_orders WHERE order_id = ?";
                    int offId = 0;
                    String newLoc = "";
                    try (PreparedStatement psF = conn.prepareStatement(fetchSql)) {
                        psF.setInt(1, orderId);
                        try (ResultSet rs = psF.executeQuery()) {
                            if (rs.next()) {
                                offId = rs.getInt("officer_id");
                                newLoc = rs.getString("new_location");
                            }
                        }
                    }

                    if (offId > 0) {
                        String sqlOff = "UPDATE officers SET current_posting = ?, years_in_current_posting = 0 WHERE officer_id = ?";
                        try (PreparedStatement psOff = conn.prepareStatement(sqlOff)) {
                            psOff.setString(1, newLoc);
                            psOff.setInt(2, offId);
                            psOff.executeUpdate();
                        }
                    }

                    conn.commit();
                    out.print("{\"status\":\"success\",\"message\":\"Joining confirmed successfully\"}");
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
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
