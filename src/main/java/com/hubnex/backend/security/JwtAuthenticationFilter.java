package com.hubnex.backend.security;

import com.hubnex.backend.model.User;
import com.hubnex.backend.repository.UserRepository;
import com.hubnex.backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/webjars");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        boolean docketRecordImportRequest = "POST".equalsIgnoreCase(request.getMethod())
                && isDocketRecordImportPath(request.getServletPath());
        boolean docketRecordRecoveryDeleteRequest = "DELETE".equalsIgnoreCase(request.getMethod())
                && isDocketRecordRecoveryDeletePath(request.getServletPath());
        boolean citiesByAgenceRequest = "GET".equalsIgnoreCase(request.getMethod())
                && isCitiesByAgencePath(request.getServletPath());
        boolean diagnosticRequest = docketRecordImportRequest || docketRecordRecoveryDeleteRequest || citiesByAgenceRequest;

        if (diagnosticRequest) {
            log.info("Docket record secured request JWT check path={} method={} bearerHeaderPresent={}",
                    request.getServletPath(),
                    request.getMethod(),
                    authHeader != null && authHeader.startsWith("Bearer "));
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (diagnosticRequest) {
                log.warn("Docket record secured request path={} method={} has no Bearer token",
                        request.getServletPath(), request.getMethod());
            }
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (tokenBlacklistService.isBlacklisted(token)) {
            if (diagnosticRequest) {
                log.warn("Docket record secured request path={} method={} uses a blacklisted token",
                        request.getServletPath(), request.getMethod());
            }
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String login = jwtService.extractLogin(token);
            if (login != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByLogin(login).orElse(null);
                if (user != null && jwtService.isTokenValid(token, user)) {
                    CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(login);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            if (diagnosticRequest) {
                log.warn("Docket record secured request JWT authentication failed path={} method={} reason={}",
                        request.getServletPath(), request.getMethod(), ex.getMessage());
            }
        }

        logDocketRecordImportAccess(request);
        logDocketRecordRecoveryDeleteAccess(request);
        logCitiesByAgenceAccess(request);
        filterChain.doFilter(request, response);
    }

    private void logDocketRecordImportAccess(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !isDocketRecordImportPath(request.getServletPath())) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("Docket record import request path={} method={} called without authenticated user",
                    request.getServletPath(),
                    request.getMethod());
            return;
        }

        log.info("Docket record import request path={} method={} user={} authorities={}",
                request.getServletPath(),
                request.getMethod(),
                authentication.getName(),
                authentication.getAuthorities());
    }

    private boolean isDocketRecordImportPath(String path) {
        return "/api/docket-records/import-excel".equals(path)
                || "/api/docket-records/import-excel/".equals(path)
                || "/api/docket-records/import-excel/preview".equals(path)
                || "/api/docket-records/import-excel/preview/".equals(path)
                || "/api/docket-records/import-excel/failed-rows/export".equals(path);
    }

    private void logDocketRecordRecoveryDeleteAccess(HttpServletRequest request) {
        if (!"DELETE".equalsIgnoreCase(request.getMethod())
                || !isDocketRecordRecoveryDeletePath(request.getServletPath())) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Docket record recovery delete path={} method={} user={} authorities={}",
                request.getServletPath(),
                request.getMethod(),
                authentication != null ? authentication.getName() : "anonymous",
                authentication != null ? authentication.getAuthorities() : java.util.List.of());
    }

    private boolean isDocketRecordRecoveryDeletePath(String path) {
        String prefix = "/api/docket-records/recoveries/";
        if (path == null || !path.startsWith(prefix)) {
            return false;
        }
        String recoveryId = path.substring(prefix.length());
        return !recoveryId.isBlank() && !recoveryId.contains("/");
    }

    private void logCitiesByAgenceAccess(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod()) || !isCitiesByAgencePath(request.getServletPath())) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Cities by agence permission check endpoint={} method={} user={} authorities={}",
                request.getServletPath(),
                request.getMethod(),
                authentication != null ? authentication.getName() : "anonymous",
                authentication != null ? authentication.getAuthorities() : java.util.List.of());
    }

    private boolean isCitiesByAgencePath(String path) {
        String prefix = "/api/cities/by-agence/";
        if (path == null || !path.startsWith(prefix)) {
            return false;
        }
        String suffix = path.substring(prefix.length());
        if (suffix.endsWith("/active")) {
            suffix = suffix.substring(0, suffix.length() - "/active".length());
        }
        return !suffix.isBlank() && !suffix.contains("/");
    }
}
