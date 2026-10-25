package com.ueclca.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/posts");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER);
        assertNotNull(requestId);
        assertEquals(36, requestId.length());
        assertNull(MDC.get("requestId"));
    }

    @Test
    void keepsValidRequestIdFromHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/posts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, "cliente-123");

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("cliente-123",
                response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER));
    }

    @Test
    void replacesInvalidRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/posts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestTraceFilter.REQUEST_ID_HEADER, "id con espacios");

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(RequestTraceFilter.REQUEST_ID_HEADER);
        assertNotNull(requestId);
        assertNotEquals("id con espacios", requestId);
    }

    @Test
    void exposesRequestContextToLogsAndClearsItAfterwards() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/posts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestId = new AtomicReference<>();
        AtomicReference<String> requestContext = new AtomicReference<>();

        FilterChain chain = (servletRequest, servletResponse) -> {
            requestId.set(MDC.get("requestId"));
            requestContext.set(MDC.get("request"));
        };
        filter.doFilter(request, response, chain);

        assertNotNull(requestId.get());
        assertEquals("GET /api/v1/posts", requestContext.get());
        assertNull(MDC.get("requestId"));
        assertNull(MDC.get("request"));
    }
}
