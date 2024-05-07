package org.cloudfoundry.multiapps.controller.web.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cloudfoundry.multiapps.controller.core.auditlogging.LoginAttemptAuditLog;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SpaceNameBasedAuthorizationFilterTest {

    private static final String ORGANIZATION_NAME = "foo";
    private static final String SPACE_NAME = "bar";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private AuthorizationChecker authorizationChecker;
    @Mock
    private LoginAttemptAuditLog loginAttemptAuditLog;
    private DummyUriAuthorizationFilter dummyUriAuthorizationFilter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(request.getRequestURI())
               .thenReturn("");
        dummyUriAuthorizationFilter = new DummyUriAuthorizationFilter(authorizationChecker, loginAttemptAuditLog);
    }

    @Test
    void testWithSuccessfulAuthorization() throws IOException {
        dummyUriAuthorizationFilter.ensureUserIsAuthorized(request, response);

        Mockito.verify(authorizationChecker)
               .ensureUserIsAuthorized(Mockito.eq(request), Mockito.any(), Mockito.eq(new CloudTarget(ORGANIZATION_NAME, SPACE_NAME)),
                                       Mockito.any());
    }

    @Test
    void testWithException() throws IOException {
        Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
               .when(authorizationChecker)
               .ensureUserIsAuthorized(Mockito.eq(request), Mockito.any(), Mockito.eq(new CloudTarget(ORGANIZATION_NAME, SPACE_NAME)),
                                       Mockito.any());

        dummyUriAuthorizationFilter.ensureUserIsAuthorized(request, response);

        Mockito.verify(response)
               .sendError(Mockito.eq(HttpStatus.FORBIDDEN.value()), Mockito.any());
    }

    private static class DummyUriAuthorizationFilter extends SpaceNameBasedAuthorizationFilter {

        public DummyUriAuthorizationFilter(AuthorizationChecker authorizationChecker, LoginAttemptAuditLog loginAttemptAuditLog) {
            super(authorizationChecker, loginAttemptAuditLog);
        }

        @Override
        public String getUriRegex() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected CloudTarget extractTarget(HttpServletRequest request) {
            return new CloudTarget(ORGANIZATION_NAME, SPACE_NAME);
        }

    }

}
