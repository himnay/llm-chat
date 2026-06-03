package com.org.llm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the configured API-key header and, when it maps to a valid key, populates the
 * {@link SecurityContextHolder} with an authenticated {@code ROLE_API_CLIENT} principal.
 *
 * <p>Missing or invalid keys are left unauthenticated — authorization rules then reject the
 * request via {@link RestAuthenticationEntryPoint} (401 JSON). This keeps the filter free of
 * response-writing concerns.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final SecurityProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = request.getHeader(properties.getHeader());
        if (key != null && !key.isBlank()
                && SecurityContextHolder.getContext().getAuthentication() == null
                && apiKeyService.isValid(key)) {
            var auth = new UsernamePasswordAuthenticationToken(
                    "api-client", null, List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
            apiKeyService.touchLastUsed(key);
        } else if (key != null && !key.isBlank()) {
            log.warn("SECURITY | rejected request to {} (invalid {})",
                    request.getRequestURI(), properties.getHeader());
        }
        chain.doFilter(request, response);
    }
}
