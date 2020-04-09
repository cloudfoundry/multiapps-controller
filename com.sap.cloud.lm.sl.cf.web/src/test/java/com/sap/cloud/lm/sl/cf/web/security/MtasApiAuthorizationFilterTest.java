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

import com.sap.cloud.lm.sl.common.SLException;

public class MtasApiAuthorizationFilterTest {

    private static final String SPACE_GUID = "e99278b1-d8a9-4b30-af52-2dfa3ea8404e";

    @Mock
    private HttpServletRequest request;
    private MtasApiAuthorizationFilter mtasApiAuthorizationFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mtasApiAuthorizationFilter = new MtasApiAuthorizationFilter(null);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/api/v1/spaces/foo/mtas", "/api/v1/spaces/foo/operations" })
    public void testUriRegexMatches(String uri) {
        assertTrue(uri.matches(mtasApiAuthorizationFilter.getUriRegex()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/public/ping", "/v1/api/spaces" })
    public void testUriRegexDoesNotMatch(String uri) {
        assertFalse(uri.matches(mtasApiAuthorizationFilter.getUriRegex()));
    }

    @Test
    public void testExtractSpaceGuid() {
        Mockito.when(request.getRequestURI())
               .thenReturn(String.format("/api/v1/spaces/%s/mtas", SPACE_GUID));
        assertEquals(SPACE_GUID, mtasApiAuthorizationFilter.extractSpaceGuid(request));
    }

    @Test
    public void testExtractSpaceGuidWithDoubleForwardSlashes() {
        Mockito.when(request.getRequestURI())
               .thenReturn(String.format("/api/////v1/spaces/%s/mtas", SPACE_GUID));
        assertEquals(SPACE_GUID, mtasApiAuthorizationFilter.extractSpaceGuid(request));
    }

    @Test
    public void testExtractSpaceGuidWithNonMatchingUri() {
        Mockito.when(request.getRequestURI())
               .thenReturn("/public/ping");
        assertThrows(SLException.class, () -> mtasApiAuthorizationFilter.extractSpaceGuid(request));
    }

}
