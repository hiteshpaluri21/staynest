package com.staynest.reservation.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String header = request.getHeader("Authorization");
        
        log.info("JwtFilter processing: {} {} | Auth header present: {}", method, uri, header != null);
        
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            log.info("Token extracted (first 20 chars): {}...", token.substring(0, Math.min(20, token.length())));
            
            try {
                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.extractEmail(token);
                    String role = jwtUtil.extractRole(token);
                    log.info("JWT valid - email: {}, role: {}, authority: ROLE_{}", email, role, role);
                    var auth = new UsernamePasswordAuthenticationToken(
                        email, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                    // Attached to the authentication so the service layer can record who acted.
                    auth.setDetails(jwtUtil.extractUserId(token));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    log.warn("JWT validation returned false for: {} {}", method, uri);
                }
            } catch (Exception e) {
                log.error("JWT processing exception for: {} {} - {}", method, uri, e.getMessage(), e);
            }
        } else {
            log.warn("No Bearer token for: {} {} | Header value: {}", method, uri, header);
        }
        
        chain.doFilter(request, response);
    }
}