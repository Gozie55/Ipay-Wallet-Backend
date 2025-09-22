package com.ipayz.ipayz_backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        log.debug("➡️ Incoming request: {}", path);

        // ✅ Skip JWT validation for public endpoints
        if (isPublicEndpoint(path)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token found for path {}, proceeding as anonymous.", path);
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        try {
            // ✅ Validate & extract claims
            Claims claims = jwtUtil.validateToken(token).getBody();
            String username = claims.getSubject();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("✅ JWT valid. Authenticated user: {}", username);
            }
        } catch (JwtException e) {
            log.warn("❌ JWT validation failed: {}", e.getMessage());
            // do not set authentication, request continues as anonymous
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/register/verify")
                || path.startsWith("/api/auth/register/google")
                || path.startsWith("/api/auth/login/initiate")
                || path.startsWith("/api/auth/login/verify")
                || path.startsWith("/api/auth/register/**")
                || path.startsWith("/uploads")
                || path.startsWith("/api/webhook/monnify"); 
    }
}
