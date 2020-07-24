package org.cloudfoundry.multiapps.controller.web.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.web.resources.CFExceptionMapper;
import org.cloudfoundry.multiapps.controller.web.security.CompositeUriAuthorizationFilter;
import org.cloudfoundry.multiapps.controller.web.security.UriAuthorizationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;

public class CompositeUriAuthorizationFilterTest {

    private static final String FOO_REQUEST_URI = "/foo/qux";
    private static final String BAR_REQUEST_URI = "/bar/qux";
    private static final String BAZ_REQUEST_URI = "/baz/qux";
    private static final String BAR_REQUEST_WITH_SLASHES_URI = "/////bar////qux";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Spy
    private FooUriAuthorizationFilter fooUriAuthorizationFilter;
    @Spy
    private BarUriAuthorizationFilter barUriAuthorizationFilter;
    private CompositeUriAuthorizationFilter compositeUriAuthorizationFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        compositeUriAuthorizationFilter = new CompositeUriAuthorizationFilter(Arrays.asList(fooUriAuthorizationFilter,
                                                                                            barUriAuthorizationFilter),
                                                                              new CFExceptionMapper());
    }

    @Test
    public void testUriMatching() throws IOException {
        Mockito.when(request.getRequestURI())
               .thenReturn(FOO_REQUEST_URI);

        assertTrue(compositeUriAuthorizationFilter.ensureUserIsAuthorized(request, response));

        Mockito.verify(fooUriAuthorizationFilter)
               .ensureUserIsAuthorized(request, response);
        Mockito.verify(barUriAuthorizationFilter, Mockito.never())
               .ensureUserIsAuthorized(request, response);
    }

    @Test
    public void testUriMatchingWithoutAnyMatchingFilters() throws IOException {
        Mockito.when(request.getRequestURI())
               .thenReturn(BAZ_REQUEST_URI);

        assertTrue(compositeUriAuthorizationFilter.ensureUserIsAuthorized(request, response));

        Mockito.verify(fooUriAuthorizationFilter, Mockito.never())
               .ensureUserIsAuthorized(request, response);
        Mockito.verify(barUriAuthorizationFilter, Mockito.never())
               .ensureUserIsAuthorized(request, response);
    }

    @Test
    public void testUriWhichContainsLotsOfForwardSlashes() throws IOException {
        Mockito.when(request.getRequestURI())
               .thenReturn(BAR_REQUEST_WITH_SLASHES_URI);

        assertFalse(compositeUriAuthorizationFilter.ensureUserIsAuthorized(request, response));

        Mockito.verify(fooUriAuthorizationFilter, Mockito.never())
               .ensureUserIsAuthorized(request, response);
        Mockito.verify(barUriAuthorizationFilter)
               .ensureUserIsAuthorized(request, response);
    }

    @Test
    public void testWithAuthorizationException() throws IOException {
        Mockito.when(request.getRequestURI())
               .thenReturn(FOO_REQUEST_URI);
        Mockito.when(fooUriAuthorizationFilter.ensureUserIsAuthorized(request, response))
               .thenThrow(new SLException("..."));
        PrintWriter writer = Mockito.mock(PrintWriter.class);
        Mockito.when(response.getWriter())
               .thenReturn(writer);

        assertFalse(compositeUriAuthorizationFilter.ensureUserIsAuthorized(request, response));

        Mockito.verify(response)
               .setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        Mockito.verify(writer)
               .print("...");
        Mockito.verify(writer)
               .flush();
    }

    @Test
    public void testWithUnauthorizedUser() throws IOException {
        Mockito.when(request.getRequestURI())
               .thenReturn(BAR_REQUEST_URI);

        assertFalse(compositeUriAuthorizationFilter.ensureUserIsAuthorized(request, response));

        Mockito.verify(barUriAuthorizationFilter)
               .ensureUserIsAuthorized(request, response);
    }

    private static class FooUriAuthorizationFilter implements UriAuthorizationFilter {

        @Override
        public String getUriRegex() {
            return "/foo/.*";
        }

        @Override
        public boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) {
            return true;
        }

    }

    private static class BarUriAuthorizationFilter implements UriAuthorizationFilter {

        @Override
        public String getUriRegex() {
            return "/bar/.*";
        }

        @Override
        public boolean ensureUserIsAuthorized(HttpServletRequest request, HttpServletResponse response) {
            return false;
        }

    }

}
