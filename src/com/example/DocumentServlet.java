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

@WebServlet("/api/document")
public class DocumentServlet extends HttpServlet {

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
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM documents WHERE request_id = ? ORDER BY document_id DESC")) {
            ps.setInt(1, reqId);
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                sb.append("{\"status\":\"success\",\"documents\":[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) sb.append(",");
                    sb.append("{");
                    sb.append("\"document_id\":").append(rs.getInt("document_id")).append(",");
                    sb.append("\"request_id\":").append(rs.getInt("request_id")).append(",");
                    sb.append("\"document_name\":\"").append(escapeJson(rs.getString("document_name"))).append("\",");
                    sb.append("\"document_type\":\"").append(escapeJson(rs.getString("document_type"))).append("\",");
                    sb.append("\"file_path\":\"").append(escapeJson(rs.getString("file_path"))).append("\",");
                    sb.append("\"uploaded_at\":\"").append(rs.getString("uploaded_at") != null ? rs.getString("uploaded_at") : "").append("\"");
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

        String reqIdStr = request.getParameter("request_id");
        String docType = request.getParameter("document_type");

        if (reqIdStr == null || reqIdStr.isEmpty()) {
            out.print("{\"status\":\"error\",\"message\":\"request_id is required\"}");
            return;
        }

        int reqId = Integer.parseInt(reqIdStr);
        String docName = (docType != null ? docType : "Attachment") + "_Doc.pdf";
        String filePath = "uploads/" + docName;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO documents (request_id, document_name, document_type, file_path) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, reqId);
            ps.setString(2, docName);
            ps.setString(3, docType != null ? docType : "OTHER");
            ps.setString(4, filePath);
            ps.executeUpdate();

            out.print("{\"status\":\"success\",\"message\":\"Document record uploaded successfully\"}");
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
