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
        if (type == null || type.isEmpty()) type = "requests";
        String export = request.getParameter("export");

        try (Connection conn = DBConnection.getConnection()) {
            if ("csv".equalsIgnoreCase(export)) {
                response.setContentType("text/csv");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + type + "_report_" + System.currentTimeMillis() + ".csv\"");
                PrintWriter out = response.getWriter();

                if ("officers".equalsIgnoreCase(type)) {
                    out.println("Officer ID,Employee ID,Officer Name,Cadre,Designation,Department,Current Posting,Current District,Years in Posting,Status");
                    String sql = "SELECT * FROM officers ORDER BY officer_id DESC";
                    try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            out.println(rs.getInt("officer_id") + ",\"" +
                                        escapeCsv(rs.getString("employee_id")) + "\",\"" +
                                        escapeCsv(rs.getString("name")) + "\",\"" +
                                        escapeCsv(rs.getString("cadre")) + "\",\"" +
                                        escapeCsv(rs.getString("designation")) + "\",\"" +
                                        escapeCsv(rs.getString("department")) + "\",\"" +
                                        escapeCsv(rs.getString("current_posting")) + "\",\"" +
                                        escapeCsv(rs.getString("current_district")) + "\"," +
                                        rs.getInt("years_in_current_posting") + ",\"" +
                                        escapeCsv(rs.getString("status")) + "\"");
                        }
                    }
                } else if ("vacancies".equalsIgnoreCase(type)) {
                    out.println("Vacancy ID,Location,District,Department,Designation,Cadre,Available Positions,Status");
                    String sql = "SELECT * FROM vacancies ORDER BY vacancy_id DESC";
                    try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            out.println(rs.getInt("vacancy_id") + ",\"" +
                                        escapeCsv(rs.getString("location")) + "\",\"" +
                                        escapeCsv(rs.getString("district")) + "\",\"" +
                                        escapeCsv(rs.getString("department")) + "\",\"" +
                                        escapeCsv(rs.getString("designation")) + "\",\"" +
                                        escapeCsv(rs.getString("cadre")) + "\"," +
                                        rs.getInt("available_positions") + ",\"" +
                                        escapeCsv(rs.getString("status")) + "\"");
                        }
                    }
                } else if ("orders".equalsIgnoreCase(type)) {
                    out.println("Order ID,Order Number,Officer Name,Employee ID,Designation,Old Location,New Location,Joining Date,Status");
                    String sql = "SELECT tor.*, o.name AS officer_name, o.employee_id, o.designation FROM transfer_orders tor JOIN officers o ON tor.officer_id = o.officer_id ORDER BY tor.order_id DESC";
                    try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            out.println(rs.getInt("order_id") + ",\"" +
                                        escapeCsv(rs.getString("order_number")) + "\",\"" +
                                        escapeCsv(rs.getString("officer_name")) + "\",\"" +
                                        escapeCsv(rs.getString("employee_id")) + "\",\"" +
                                        escapeCsv(rs.getString("designation")) + "\",\"" +
                                        escapeCsv(rs.getString("old_location")) + "\",\"" +
                                        escapeCsv(rs.getString("new_location")) + "\",\"" +
                                        escapeCsv(rs.getString("joining_date")) + "\",\"" +
                                        escapeCsv(rs.getString("status")) + "\"");
                        }
                    }
                } else { // requests
                    out.println("Request ID,Officer Name,Cadre,Current Posting,Preferred Location 1,Status,Priority,Score");
                    String sql = "SELECT tr.*, o.name AS officer_name, o.cadre, o.current_posting FROM transfer_requests tr JOIN officers o ON tr.officer_id = o.officer_id ORDER BY tr.request_id DESC";
                    try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            out.println(rs.getInt("request_id") + ",\"" +
                                        escapeCsv(rs.getString("officer_name")) + "\",\"" +
                                        escapeCsv(rs.getString("cadre")) + "\",\"" +
                                        escapeCsv(rs.getString("current_posting")) + "\",\"" +
                                        escapeCsv(rs.getString("preferred_location_1")) + "\",\"" +
                                        escapeCsv(rs.getString("status")) + "\",\"" +
                                        escapeCsv(rs.getString("priority")) + "\"," +
                                        rs.getInt("optimization_score"));
                        }
                    }
                }
                out.flush();
                return;
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            PrintWriter out = response.getWriter();

            StringBuilder sb = new StringBuilder();
            sb.append("{\"status\":\"success\",\"type\":\"").append(escapeJson(type)).append("\",\"data\":[");

            if ("officers".equalsIgnoreCase(type)) {
                String sql = "SELECT * FROM officers ORDER BY officer_id DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        sb.append("{");
                        sb.append("\"officer_id\":").append(rs.getInt("officer_id")).append(",");
                        sb.append("\"employee_id\":\"").append(escapeJson(rs.getString("employee_id"))).append("\",");
                        sb.append("\"name\":\"").append(escapeJson(rs.getString("name"))).append("\",");
                        sb.append("\"cadre\":\"").append(escapeJson(rs.getString("cadre"))).append("\",");
                        sb.append("\"designation\":\"").append(escapeJson(rs.getString("designation"))).append("\",");
                        sb.append("\"department\":\"").append(escapeJson(rs.getString("department"))).append("\",");
                        sb.append("\"current_posting\":\"").append(escapeJson(rs.getString("current_posting"))).append("\",");
                        sb.append("\"current_district\":\"").append(escapeJson(rs.getString("current_district"))).append("\",");
                        sb.append("\"years_in_current_posting\":").append(rs.getInt("years_in_current_posting")).append(",");
                        sb.append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\"");
                        sb.append("}");
                        first = false;
                    }
                }
            } else if ("vacancies".equalsIgnoreCase(type)) {
                String sql = "SELECT * FROM vacancies ORDER BY vacancy_id DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        sb.append("{");
                        sb.append("\"vacancy_id\":").append(rs.getInt("vacancy_id")).append(",");
                        sb.append("\"location\":\"").append(escapeJson(rs.getString("location"))).append("\",");
                        sb.append("\"district\":\"").append(escapeJson(rs.getString("district"))).append("\",");
                        sb.append("\"department\":\"").append(escapeJson(rs.getString("department"))).append("\",");
                        sb.append("\"designation\":\"").append(escapeJson(rs.getString("designation"))).append("\",");
                        sb.append("\"cadre\":\"").append(escapeJson(rs.getString("cadre"))).append("\",");
                        sb.append("\"available_positions\":").append(rs.getInt("available_positions")).append(",");
                        sb.append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\"");
                        sb.append("}");
                        first = false;
                    }
                }
            } else if ("orders".equalsIgnoreCase(type)) {
                String sql = "SELECT tor.*, o.name AS officer_name, o.employee_id, o.designation FROM transfer_orders tor JOIN officers o ON tor.officer_id = o.officer_id ORDER BY tor.order_id DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) sb.append(",");
                        sb.append("{");
                        sb.append("\"order_id\":").append(rs.getInt("order_id")).append(",");
                        sb.append("\"order_number\":\"").append(escapeJson(rs.getString("order_number"))).append("\",");
                        sb.append("\"officer_name\":\"").append(escapeJson(rs.getString("officer_name"))).append("\",");
                        sb.append("\"employee_id\":\"").append(escapeJson(rs.getString("employee_id"))).append("\",");
                        sb.append("\"designation\":\"").append(escapeJson(rs.getString("designation"))).append("\",");
                        sb.append("\"old_location\":\"").append(escapeJson(rs.getString("old_location"))).append("\",");
                        sb.append("\"new_location\":\"").append(escapeJson(rs.getString("new_location"))).append("\",");
                        sb.append("\"joining_date\":\"").append(rs.getString("joining_date") != null ? rs.getString("joining_date") : "").append("\",");
                        sb.append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\"");
                        sb.append("}");
                        first = false;
                    }
                }
            } else { // requests
                String sql = "SELECT tr.*, o.name AS officer_name, o.employee_id, o.cadre, o.current_posting FROM transfer_requests tr JOIN officers o ON tr.officer_id = o.officer_id ORDER BY tr.request_id DESC";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
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
                }
            }
            sb.append("]}");
            out.print(sb.toString());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.getWriter().print("{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
