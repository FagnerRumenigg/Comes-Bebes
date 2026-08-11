package org.application.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private Logger filterLogger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        filterLogger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
        originalLevel = filterLogger.getLevel();
        filterLogger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        filterLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        filterLogger.detachAppender(appender);
        filterLogger.setLevel(originalLevel);
    }

    @Test
    void shouldLogGetRequestsAtDebugOnly() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/publications/feed");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.DEBUG);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("event=http_request", "method=GET");
    }

    @Test
    void shouldLogMutationRequestsAtInfoWithStatus() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/publications");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.get(0).getFormattedMessage())
                .contains("event=http_request", "method=POST", "status=201");
    }
}
