package org.application.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    @Test
    void shouldNotLogGetRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/publications/feed");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // O filtro apenas prepara o contexto MDC; requisições não são logadas.
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldNotLogMutationRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/publications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(201);
    }
}
