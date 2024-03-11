package com.sap.cloud.lm.sl.cf.web.security;

import static com.sap.cloud.lm.sl.cf.web.security.CustomAccessDeniedHandler.CSRF_TOKEN_REQUIRED_HEADER_VALUE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

class CustomAccessDeniedHandlerTest {

    private final CustomAccessDeniedHandler customAccessDeniedHandler = new CustomAccessDeniedHandler();

    @Test
    void testAccessDeniedWithInvalidCsrfException() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        CsrfToken csrfToken = mock(CsrfToken.class);
        customAccessDeniedHandler.handle(httpRequest, httpResponse, new InvalidCsrfTokenException(csrfToken, "actual"));
        verify(httpResponse).setHeader(CsrfHeaders.CSRF_TOKEN_HEADER, CSRF_TOKEN_REQUIRED_HEADER_VALUE);
        verify(httpResponse).setStatus(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    void testAccessDeniedWithMissingCsrfException() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        customAccessDeniedHandler.handle(httpRequest, httpResponse, new MissingCsrfTokenException("actual"));
        verify(httpResponse).setHeader(CsrfHeaders.CSRF_TOKEN_HEADER, CSRF_TOKEN_REQUIRED_HEADER_VALUE);
        verify(httpResponse).setStatus(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    void testAccessDeniedWithRandomException() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        customAccessDeniedHandler.handle(httpRequest, httpResponse, new AuthorizationServiceException("actual"));
        verify(httpResponse, never()).setHeader(CsrfHeaders.CSRF_TOKEN_HEADER, CSRF_TOKEN_REQUIRED_HEADER_VALUE);
        verify(httpResponse).setStatus(HttpStatus.SC_FORBIDDEN);
    }

}
