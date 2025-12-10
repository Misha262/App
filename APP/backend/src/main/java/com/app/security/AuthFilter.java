package com.app.security;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Simple JWT filter: allows /api/auth/* without token, checks Bearer for others.
 */
@WebFilter("/api/*")
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        resp.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");

        String path = req.getRequestURI();
        String ctx = req.getContextPath();
        String authPrefix = ctx + "/api/auth/";

        if (path.startsWith(authPrefix)) {
            chain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        Map<String, Object> payload = JwtUtil.validateToken(token);
        if (payload == null) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Invalid or expired token\"}");
            return;
        }

        Object sub = payload.get("sub");
        Object email = payload.get("email");
        int userId;
        try {
            userId = Integer.parseInt(String.valueOf(sub));
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Invalid token payload\"}");
            return;
        }

        req.setAttribute("userId", userId);
        req.setAttribute("userEmail", email != null ? email.toString() : null);

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // no-op
    }
}
