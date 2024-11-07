package org.cloudfoundry.multiapps.controller.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.auditlogging.LoginAttemptAuditLog;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class AdminApiAuthorizationFilterTest {

    private static final String SPACE_GUID = "e99278b1-d8a9-4b30-af52-2dfa3ea8404e";

    @Mock
    private HttpServletRequest request;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private LoginAttemptAuditLog loginAttemptAuditLog;
    private AdminApiAuthorizationFilter adminApiAuthorizationFilter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        adminApiAuthorizationFilter = new AdminApiAuthorizationFilter(applicationConfiguration, null, loginAttemptAuditLog);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/rest/admin/statistics", "/rest/admin/shutdown" })
    void testUriRegexMatches(String uri) {
        assertTrue(uri.matches(adminApiAuthorizationFilter.getUriRegex()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/admin", "/public/ping", "/rest/configuration-subscriptions" })
    void testUriRegexDoesNotMatch(String uri) {
        assertFalse(uri.matches(adminApiAuthorizationFilter.getUriRegex()));
    }

    @Test
    void testExtractSpaceGuid() {
        Mockito.when(applicationConfiguration.getSpaceGuid())
               .thenReturn(SPACE_GUID);
        assertEquals(SPACE_GUID, adminApiAuthorizationFilter.extractSpaceGuid(request));
    }

    @Test
    void testExtractSpaceGuidWithEmptyString() {
        Mockito.when(applicationConfiguration.getSpaceGuid())
               .thenReturn("");
        assertThrows(SLException.class, () -> adminApiAuthorizationFilter.extractSpaceGuid(request));
    }

}
