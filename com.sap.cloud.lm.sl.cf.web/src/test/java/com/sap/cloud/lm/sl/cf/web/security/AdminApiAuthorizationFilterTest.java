package com.sap.cloud.lm.sl.cf.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

public class AdminApiAuthorizationFilterTest {

    private static final String SPACE_GUID = "e99278b1-d8a9-4b30-af52-2dfa3ea8404e";

    @Mock
    private HttpServletRequest request;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    private AdminApiAuthorizationFilter adminApiAuthorizationFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        adminApiAuthorizationFilter = new AdminApiAuthorizationFilter(applicationConfiguration, null);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/rest/admin/statistics", "/rest/admin/shutdown" })
    public void testUriRegexMatches(String uri) {
        assertTrue(uri.matches(adminApiAuthorizationFilter.getUriRegex()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/admin", "/public/ping", "/rest/configuration-subscriptions" })
    public void testUriRegexDoesNotMatch(String uri) {
        assertFalse(uri.matches(adminApiAuthorizationFilter.getUriRegex()));
    }

    @Test
    public void testExtractSpaceGuid() {
        Mockito.when(applicationConfiguration.getSpaceGuid())
               .thenReturn(SPACE_GUID);
        assertEquals(SPACE_GUID, adminApiAuthorizationFilter.extractSpaceGuid(request));
    }

    @Test
    public void testExtractSpaceGuidWithEmptyString() {
        Mockito.when(applicationConfiguration.getSpaceGuid())
               .thenReturn("");
        AuthorizationException authorizationException = assertThrows(AuthorizationException.class,
                                                                     () -> adminApiAuthorizationFilter.extractSpaceGuid(request));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), authorizationException.getStatusCode());
    }

}
