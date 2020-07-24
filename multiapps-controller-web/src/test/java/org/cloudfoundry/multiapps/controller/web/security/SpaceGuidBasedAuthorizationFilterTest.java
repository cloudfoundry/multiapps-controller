package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.web.security.AuthorizationChecker;
import org.cloudfoundry.multiapps.controller.web.security.SpaceGuidBasedAuthorizationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class SpaceGuidBasedAuthorizationFilterTest {

    private static final String SPACE_GUID = "e99278b1-d8a9-4b30-af52-2dfa3ea8404e";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private AuthorizationChecker authorizationChecker;
    private DummyUriAuthorizationFilter dummyUriAuthorizationFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(request.getRequestURI())
               .thenReturn("");
        dummyUriAuthorizationFilter = new DummyUriAuthorizationFilter(authorizationChecker);
    }

    @Test
    public void testWithSuccessfulAuthorization() throws IOException {
        dummyUriAuthorizationFilter.ensureUserIsAuthorized(request, response);

        Mockito.verify(authorizationChecker)
               .ensureUserIsAuthorized(Mockito.eq(request), Mockito.any(), Mockito.eq(SPACE_GUID), Mockito.any());
    }

    @Test
    public void testWithException() throws IOException {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
               .when(authorizationChecker)
               .ensureUserIsAuthorized(Mockito.eq(request), Mockito.any(), Mockito.eq(SPACE_GUID), Mockito.any());

        dummyUriAuthorizationFilter.ensureUserIsAuthorized(request, response);

        Mockito.verify(response)
               .sendError(Mockito.eq(HttpStatus.UNAUTHORIZED.value()), Mockito.any());
    }

    private static class DummyUriAuthorizationFilter extends SpaceGuidBasedAuthorizationFilter {

        public DummyUriAuthorizationFilter(AuthorizationChecker authorizationChecker) {
            super(authorizationChecker);
        }

        @Override
        public String getUriRegex() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected String extractSpaceGuid(HttpServletRequest request) {
            return SPACE_GUID;
        }

    }

}
