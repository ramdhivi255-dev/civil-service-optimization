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

@WebServlet("/api/report")
public class ReportServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.setContentType("application/json");
            response.getWriter().print("{\"status\":\"unauthenticated\"}");
            return;
        }

        String type = request.getParameter("type");
        String export = request.getParameter("export");

        if ("csv".equalsIgnoreCase(export)) {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=\"transfer_report_" + System.currentTimeMillis() + ".csv\"");
            PrintWriter out = response.getWriter();
            out.println("Request ID,Officer Name,Cadre,Current Posting,Preferred Location 1,Status,Priority,Score");

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT tr.*, o.name AS officer_name, o.cadre, o.current_posting FROM transfer_requests tr JOIN officers o ON tr.officer_id = o.officer_id ORDER BY tr.request_id DESC");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.println(rs.getInt("request_id") + ",\"" +
                                rs.getString("officer_name") + "\",\"" +
                                rs.getString("cadre") + "\",\"" +
                                rs.getString("current_posting") + "\",\"" +
                                rs.getString("preferred_location_1") + "\",\"" +
                                rs.getString("status") + "\",\"" +
                                rs.getString("priority") + "\"," +
                                rs.getInt("optimization_score"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            out.flush();
            return;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT tr.*, o.name AS officer_name, o.employee_id, o.cadre, o.current_posting FROM transfer_requests tr JOIN officers o ON tr.officer_id = o.officer_id ORDER BY tr.request_id DESC";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                sb.append("{\"status\":\"success\",\"type\":\"").append(escapeJson(type != null ? type : "all")).append("\",\"data\":[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{");
                    sb.append("\"request_id\":").append(rs.getInt("request_id")).append(",");
                    sb.append("\"officer_name\":\"").append(escapeJson(rs.getString("officer_name"))).append("\",");
                    sb.append("\"employee_id\":\"").append(escapeJson(rs.getString("employee_id"))).append("\",");
                    sb.append("\"cadre\":\"").append(escapeJson(rs.getString("cadre"))).append("\",");
                    sb.append("\"current_posting\":\"").append(escapeJson(rs.getString("current_posting"))).append("\",");
                    sb.append("\"preferred_location_1\":\"").append(escapeJson(rs.getString("preferred_location_1"))).append("\",");
                    sb.append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\",");
                    sb.append("\"priority\":\"").append(escapeJson(rs.getString("priority"))).append("\",");
                    sb.append("\"optimization_score\":").append(rs.getInt("optimization_score"));
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

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
