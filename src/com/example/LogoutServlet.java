package com.example;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/api/logout")
public class LogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Integer userId = (Integer) session.getAttribute("user_id");
            String username = (String) session.getAttribute("username");
            String role = (String) session.getAttribute("role");
            if (userId != null) {
                DBConnection.logAudit(null, userId, role, "LOGOUT", "users", userId, "User logged out: " + (username != null ? username : "User"));
            }
            session.invalidate();
        }
        response.setContentType("application/json");
        response.getWriter().print("{\"status\":\"success\",\"message\":\"Logged out successfully\"}");
    }
}
