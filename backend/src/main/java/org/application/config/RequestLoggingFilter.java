package org.application.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RequestLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        putAuthenticationContext(request);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("username");
            MDC.remove("sessionId");
        }
    }

    private void putAuthenticationContext(HttpServletRequest request) {
        Authentication authentication = (Authentication) request.getUserPrincipal();
        if (authentication == null) {
            return;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Object username = jwtAuthentication.getTokenAttributes().get("username");
            if (username != null) {
                MDC.put("username", username.toString());
            }
            Object sessionId = jwtAuthentication.getTokenAttributes().get("session_id");
            if (sessionId != null) {
                MDC.put("sessionId", sessionId.toString());
            }
        }
    }
}
