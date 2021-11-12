package org.cloudfoundry.multiapps.controller.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class DefaultSpaceGuidBasedAuthorizationFilterTest {

    private static final String SPACE_GUID = "e99278b1-d8a9-4b30-af52-2dfa3ea8404e";

    @Mock
    private HttpServletRequest request;
    private DefaultSpaceGuidBasedAuthorizationFilter defaultSpaceGuidBasedAuthorizationFilter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        defaultSpaceGuidBasedAuthorizationFilter = new DefaultSpaceGuidBasedAuthorizationFilter(null);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/v1/spaces/foo/mtas", "/api/v1/spaces/foo/operations", "/api/v2/spaces/foo/mtas" })
    void testUriRegexMatches(String uri) {
        assertTrue(uri.matches(defaultSpaceGuidBasedAuthorizationFilter.getUriRegex()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/public/ping", "/v1/api/spaces" })
    void testUriRegexDoesNotMatch(String uri) {
        assertFalse(uri.matches(defaultSpaceGuidBasedAuthorizationFilter.getUriRegex()));
    }

    @Test
    void testExtractSpaceGuid() {
        Mockito.when(request.getRequestURI())
               .thenReturn(String.format("/api/v1/spaces/%s/mtas", SPACE_GUID));
        assertEquals(SPACE_GUID, defaultSpaceGuidBasedAuthorizationFilter.extractSpaceGuid(request));
    }

    @Test
    void testExtractSpaceGuidWithDoubleForwardSlashes() {
        Mockito.when(request.getRequestURI())
               .thenReturn(String.format("/api/////v1/spaces/%s/mtas", SPACE_GUID));
        assertEquals(SPACE_GUID, defaultSpaceGuidBasedAuthorizationFilter.extractSpaceGuid(request));
    }

    @Test
    void testExtractSpaceGuidWithNonMatchingUri() {
        Mockito.when(request.getRequestURI())
               .thenReturn("/public/ping");
        assertThrows(SLException.class, () -> defaultSpaceGuidBasedAuthorizationFilter.extractSpaceGuid(request));
    }

}
