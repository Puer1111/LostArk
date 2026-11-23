package com.lostark.lostark.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class CsrfTokenMaterializationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Object csrfTokenAttribute = request.getAttribute(CsrfToken.class.getName());
        if (csrfTokenAttribute instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
