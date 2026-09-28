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

@WebServlet({"/api/vacancy", "/vacancy"})
public class VacancyServlet extends HttpServlet {

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

        String search = request.getParameter("search");
        String district = request.getParameter("district");

        StringBuilder sql = new StringBuilder("SELECT * FROM vacancies WHERE 1=1");
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (location LIKE ? OR designation LIKE ? OR department LIKE ?)");
        }
        if (district != null && !district.trim().isEmpty() && !"ALL".equalsIgnoreCase(district)) {
            sql.append(" AND district = ?");
        }
        sql.append(" ORDER BY vacancy_id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int pIndex = 1;
            if (search != null && !search.trim().isEmpty()) {
                String pat = "%" + search.trim() + "%";
                ps.setString(pIndex++, pat);
                ps.setString(pIndex++, pat);
                ps.setString(pIndex++, pat);
            }
            if (district != null && !district.trim().isEmpty() && !"ALL".equalsIgnoreCase(district)) {
                ps.setString(pIndex++, district.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                sb.append("{\"status\":\"success\",\"vacancies\":[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{");
                    sb.append("\"vacancy_id\":").append(rs.getInt("vacancy_id")).append(",");
                    sb.append("\"location\":\"").append(escapeJson(rs.getString("location"))).append("\",");
                    sb.append("\"district\":\"").append(escapeJson(rs.getString("district"))).append("\",");
                    sb.append("\"state\":\"").append(escapeJson(rs.getString("state"))).append("\",");
                    sb.append("\"department\":\"").append(escapeJson(rs.getString("department"))).append("\",");
                    sb.append("\"designation\":\"").append(escapeJson(rs.getString("designation"))).append("\",");
                    sb.append("\"cadre\":\"").append(escapeJson(rs.getString("cadre"))).append("\",");
                    sb.append("\"available_positions\":").append(rs.getInt("available_positions")).append(",");
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
            if ("add".equals(action) || "create".equals(action)) {
                String loc = request.getParameter("location");
                String dist = request.getParameter("district");
                String dept = request.getParameter("department");
                String des = request.getParameter("designation");
                String cadre = request.getParameter("cadre");
                String posStr = request.getParameter("available_positions");

                int pos = posStr != null && !posStr.isEmpty() ? Integer.parseInt(posStr) : 1;

                String sql = "INSERT INTO vacancies (location, district, department, designation, cadre, available_positions, status) VALUES (?, ?, ?, ?, ?, ?, 'AVAILABLE')";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, loc);
                    ps.setString(2, dist);
                    ps.setString(3, dept);
                    ps.setString(4, des);
                    ps.setString(5, cadre);
                    ps.setInt(6, pos);
                    ps.executeUpdate();
                }
                out.print("{\"status\":\"success\",\"message\":\"Vacancy added successfully\"}");
            } else if ("delete".equals(action)) {
                String vacIdStr = request.getParameter("vacancy_id");
                if (vacIdStr != null && !vacIdStr.isEmpty()) {
                    String sql = "DELETE FROM vacancies WHERE vacancy_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, Integer.parseInt(vacIdStr));
                        ps.executeUpdate();
                    }
                }
                out.print("{\"status\":\"success\",\"message\":\"Vacancy removed\"}");
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
