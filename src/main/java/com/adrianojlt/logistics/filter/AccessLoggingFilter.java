package com.adrianojlt.logistics.filter;

import com.adrianojlt.logistics.security.JwtUtil;
import com.adrianojlt.logistics.util.WebUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccessLoggingFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public AccessLoggingFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        long start = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            log.info("http_request method={} uri={} user={} ip={} status={} duration_ms={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    resolveUsername(request),
                    WebUtil.resolveClientIp(request),
                    response.getStatus(),
                    System.currentTimeMillis() - start);
        }
    }

    private String resolveUsername(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return "anonymous";
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!jwtUtil.isTokenValid(token)) {
            return "anonymous";
        }

        return jwtUtil.extractUsername(token);
    }
}
