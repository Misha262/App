package com.app.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Map;

/**
 * Фильтр, который:
 * 1) читает заголовок Authorization
 * 2) валидирует JWT
 * 3) кладёт userId в request.setAttribute("userId")
 */
public class JwtFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req,
                         ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;

        String auth = request.getHeader("Authorization");

        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);

            Map<String, Object> payload = JwtUtil.validateToken(token);

            if (payload != null) {
                Object sub = payload.get("sub");
                if (sub != null) {
                    try {
                        int userId = Integer.parseInt(sub.toString());
                        request.setAttribute("userId", userId);
                    } catch (Exception ignored) {
                        // если вдруг userId не int — просто игнорируем
                    }
                }
            }
        }

        chain.doFilter(req, res);
    }
}
