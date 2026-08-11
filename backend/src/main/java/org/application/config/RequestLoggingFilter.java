package org.application.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.nanoTime();
        MDC.put("requestId", UUID.randomUUID().toString());
        putAuthenticationContext(request);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            if (isMutation(request.getMethod())) {
                log.info("event=http_request method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            } else if (log.isDebugEnabled()) {
                log.debug("event=http_request method={} path={} status={} durationMs={}",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
            }
            MDC.remove("requestId");
            MDC.remove("sessionId");
            MDC.remove("userId");
        }
    }

    private boolean isMutation(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method);
    }

    private void putAuthenticationContext(HttpServletRequest request) {
        Authentication authentication = (Authentication) request.getUserPrincipal();
        if (authentication == null) {
            return;
        }
        if (authentication.getName() != null) {
            MDC.put("userId", authentication.getName());
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Object sessionId = jwtAuthentication.getTokenAttributes().get("session_id");
            if (sessionId != null) {
                MDC.put("sessionId", sessionId.toString());
            }
        }
    }
}
