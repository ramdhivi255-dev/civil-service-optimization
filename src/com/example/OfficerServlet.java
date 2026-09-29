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

@WebServlet("/api/officer")
public class OfficerServlet extends HttpServlet {

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
        if (action == null) action = "get_profile";

        try (Connection conn = DBConnection.getConnection()) {
            if ("get_profile".equals(action)) {
                int userId = (Integer) session.getAttribute("user_id");
                String sql = "SELECT o.*, u.username FROM officers o JOIN users u ON o.user_id = u.user_id WHERE o.user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            out.print("{\"status\":\"success\",\"officer\":" + buildOfficerJson(rs) + "}");
                        } else {
                            out.print("{\"status\":\"error\",\"message\":\"Officer profile not found\"}");
                        }
                    }
                }
            } else if ("list".equals(action)) {
                String search = request.getParameter("search");
                String cadre = request.getParameter("cadre");

                StringBuilder sql = new StringBuilder("SELECT o.*, u.username FROM officers o JOIN users u ON o.user_id = u.user_id WHERE 1=1");
                if (search != null && !search.trim().isEmpty()) {
                    sql.append(" AND (o.name LIKE ? OR o.employee_id LIKE ? OR o.current_posting LIKE ? OR o.current_district LIKE ?)");
                }
                if (cadre != null && !cadre.trim().isEmpty() && !"ALL".equalsIgnoreCase(cadre)) {
                    sql.append(" AND o.cadre = ?");
                }
                sql.append(" ORDER BY o.officer_id DESC");

                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    int pIndex = 1;
                    if (search != null && !search.trim().isEmpty()) {
                        String pattern = "%" + search.trim() + "%";
                        ps.setString(pIndex++, pattern);
                        ps.setString(pIndex++, pattern);
                        ps.setString(pIndex++, pattern);
                        ps.setString(pIndex++, pattern);
                    }
                    if (cadre != null && !cadre.trim().isEmpty() && !"ALL".equalsIgnoreCase(cadre)) {
                        ps.setString(pIndex++, cadre.trim());
                    }

                    try (ResultSet rs = ps.executeQuery()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("{\"status\":\"success\",\"officers\":[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) sb.append(",");
                            sb.append(buildOfficerJson(rs));
                            first = false;
                        }
                        sb.append("]}");
                        out.print(sb.toString());
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

        try (Connection conn = DBConnection.getConnection()) {
            if ("add".equals(action)) {
                String username = request.getParameter("username");
                String password = request.getParameter("password");
                String empId = request.getParameter("employee_id");
                String name = request.getParameter("name");
                String email = request.getParameter("email");
                String phone = request.getParameter("phone");
                String des = request.getParameter("designation");
                String dept = request.getParameter("department");
                String cadre = request.getParameter("cadre");
                String posting = request.getParameter("current_posting");
                String district = request.getParameter("current_district");
                String yrs = request.getParameter("years_in_current_posting");

                conn.setAutoCommit(false);
                try {
                    String userSql = "INSERT INTO users (username, password, name, email, role) VALUES (?, ?, ?, ?, 'CIVIL_SERVICE_OFFICER')";
                    int newUserId = 0;
                    try (PreparedStatement psUser = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                        psUser.setString(1, username);
                        psUser.setString(2, password);
                        psUser.setString(3, name);
                        psUser.setString(4, email);
                        psUser.executeUpdate();
                        try (ResultSet rs = psUser.getGeneratedKeys()) {
                            if (rs.next()) newUserId = rs.getInt(1);
                        }
                    }

                    String offSql = "INSERT INTO officers (user_id, employee_id, name, email, phone, designation, department, cadre, current_posting, current_district, years_in_current_posting) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement psOff = conn.prepareStatement(offSql)) {
                        psOff.setInt(1, newUserId);
                        psOff.setString(2, empId);
                        psOff.setString(3, name);
                        psOff.setString(4, email);
                        psOff.setString(5, phone);
                        psOff.setString(6, des);
                        psOff.setString(7, dept);
                        psOff.setString(8, cadre);
                        psOff.setString(9, posting);
                        psOff.setString(10, district);
                        psOff.setInt(11, yrs != null && !yrs.isEmpty() ? Integer.parseInt(yrs) : 0);
                        psOff.executeUpdate();
                    }
                    conn.commit();
                    out.print("{\"status\":\"success\",\"message\":\"Officer added successfully\"}");
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }
            } else if ("update_profile".equals(action) || "edit".equals(action)) {
                int userId = (Integer) session.getAttribute("user_id");
                String phone = request.getParameter("phone");
                String email = request.getParameter("email");
                String posting = request.getParameter("current_posting");
                String district = request.getParameter("current_district");
                String yrs = request.getParameter("years_in_current_posting");

                String sql = "UPDATE officers SET phone = ?, email = ?, current_posting = ?, current_district = ?, years_in_current_posting = ? WHERE user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, phone);
                    ps.setString(2, email);
                    ps.setString(3, posting);
                    ps.setString(4, district);
                    ps.setInt(5, yrs != null && !yrs.isEmpty() ? Integer.parseInt(yrs) : 0);
                    ps.setInt(6, userId);
                    ps.executeUpdate();
                }
                out.print("{\"status\":\"success\",\"message\":\"Profile updated successfully\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
        out.flush();
    }

    private String buildOfficerJson(ResultSet rs) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"officer_id\":").append(rs.getInt("officer_id")).append(",");
        sb.append("\"user_id\":").append(rs.getInt("user_id")).append(",");
        sb.append("\"employee_id\":\"").append(escapeJson(rs.getString("employee_id"))).append("\",");
        sb.append("\"name\":\"").append(escapeJson(rs.getString("name"))).append("\",");
        sb.append("\"email\":\"").append(escapeJson(rs.getString("email"))).append("\",");
        sb.append("\"phone\":\"").append(escapeJson(rs.getString("phone"))).append("\",");
        sb.append("\"designation\":\"").append(escapeJson(rs.getString("designation"))).append("\",");
        sb.append("\"department\":\"").append(escapeJson(rs.getString("department"))).append("\",");
        sb.append("\"cadre\":\"").append(escapeJson(rs.getString("cadre"))).append("\",");
        sb.append("\"current_posting\":\"").append(escapeJson(rs.getString("current_posting"))).append("\",");
        sb.append("\"current_district\":\"").append(escapeJson(rs.getString("current_district"))).append("\",");
        sb.append("\"years_in_current_posting\":").append(rs.getInt("years_in_current_posting")).append(",");
        sb.append("\"status\":\"").append(escapeJson(rs.getString("status"))).append("\"");
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
