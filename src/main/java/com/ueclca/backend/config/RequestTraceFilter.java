package com.ueclca.backend.config;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestTraceFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    private static final String REQUEST_ID_ATTRIBUTE = RequestTraceFilter.class.getName() + ".requestId";
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put("requestId", requestId);
        MDC.put("request", request.getMethod() + " " + request.getRequestURI());
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.debug("{} {} -> {} en {} ms", request.getMethod(), request.getRequestURI(),
                    response.getStatus(), (System.nanoTime() - startedAt) / 1_000_000);
            MDC.remove("requestId");
            MDC.remove("request");
        }
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String value) {
            return value;
        }

        String header = request.getHeader(REQUEST_ID_HEADER);
        String requestId = header != null && VALID_REQUEST_ID.matcher(header).matches()
                ? header
                : UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        return requestId;
    }
}
