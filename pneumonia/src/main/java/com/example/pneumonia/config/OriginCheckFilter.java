package com.example.pneumonia.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private final Set<String> allowed;

    public OriginCheckFilter(Set<String> allowed) {
        this.allowed = allowed;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String origin = req.getHeader("Origin");
        if (origin != null && !SAFE.contains(req.getMethod())
                && !allowed.contains(origin) && !origin.equals(selfOrigin(req))) {
            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().write("Origine non autorisée.");
            return;
        }
        chain.doFilter(req, res);
    }

    private static String selfOrigin(HttpServletRequest r) {
        int port = r.getServerPort();
        boolean def = ("http".equals(r.getScheme()) && port == 80) || ("https".equals(r.getScheme()) && port == 443);
        return r.getScheme() + "://" + r.getServerName() + (def ? "" : ":" + port);
    }
}