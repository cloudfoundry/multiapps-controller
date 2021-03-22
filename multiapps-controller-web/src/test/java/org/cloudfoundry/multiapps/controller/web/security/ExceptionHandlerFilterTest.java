package org.cloudfoundry.multiapps.controller.web.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.server.ResponseStatusException;

class ExceptionHandlerFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private final ExceptionHandlerFilter exceptionHandlerFilter = new ExceptionHandlerFilter();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testDoInternalFilterWithoutException() throws IOException, ServletException {
        assertDoesNotThrow(() -> exceptionHandlerFilter.doFilterInternal(request, response, filterChain));
        Mockito.verify(filterChain)
               .doFilter(request, response);
    }

    @Test
    void testDoInternalFilterThrowingResponseStatusException() throws IOException, ServletException {
        ResponseStatusException responseStatusException = Mockito.mock(ResponseStatusException.class);
        Mockito.when(responseStatusException.getStatus())
               .thenReturn(HttpStatus.UNAUTHORIZED);
        Mockito.when(responseStatusException.getMessage())
               .thenReturn("Error");
        PrintWriter printWriter = Mockito.mock(PrintWriter.class);
        Mockito.when(response.getWriter())
               .thenReturn(printWriter);
        Mockito.doThrow(responseStatusException)
               .when(filterChain)
               .doFilter(request, response);
        assertDoesNotThrow(() -> exceptionHandlerFilter.doFilterInternal(request, response, filterChain));
        Mockito.verify(response)
               .setStatus(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void testInternalFilterThrowingInsufficientAuthenticationException() throws IOException, ServletException {
        InsufficientAuthenticationException insufficientAuthenticationException = Mockito.mock(InsufficientAuthenticationException.class);
        Mockito.doThrow(insufficientAuthenticationException)
               .when(filterChain)
               .doFilter(request, response);
        assertDoesNotThrow(() -> exceptionHandlerFilter.doFilterInternal(request, response, filterChain));
        Mockito.verify(response)
               .setStatus(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void testInternalAuthenticationServiceExceptionException() throws IOException, ServletException {
        InternalAuthenticationServiceException internalAuthenticationServiceException = Mockito.mock(InternalAuthenticationServiceException.class);
        Mockito.doThrow(internalAuthenticationServiceException)
               .when(filterChain)
               .doFilter(request, response);
        assertDoesNotThrow(() -> exceptionHandlerFilter.doFilterInternal(request, response, filterChain));
        Mockito.verify(response)
               .setStatus(HttpStatus.UNAUTHORIZED.value());
    }

}
