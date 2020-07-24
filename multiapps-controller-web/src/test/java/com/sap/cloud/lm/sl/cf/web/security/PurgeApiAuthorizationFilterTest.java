package com.sap.cloud.lm.sl.cf.web.security;

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

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.web.resources.ConfigurationEntriesResource;

public class PurgeApiAuthorizationFilterTest {

    private static final String ORGANIZATION_NAME = "foo";
    private static final String SPACE_NAME = "bar";

    @Mock
    private HttpServletRequest request;
    private PurgeApiAuthorizationFilter purgeApiAuthorizationFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        purgeApiAuthorizationFilter = new PurgeApiAuthorizationFilter(null);
    }

    @Test
    public void testUriRegexMatches() {
        assertTrue("/rest/configuration-entries/purge".matches(purgeApiAuthorizationFilter.getUriRegex()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/rest/configuration-entries", "/v1/api/spaces" })
    public void testUriRegexDoesNotMatch(String uri) {
        assertFalse(uri.matches(purgeApiAuthorizationFilter.getUriRegex()));
    }

    @Test
    public void testExtractTarget() {
        Mockito.when(request.getParameter(ConfigurationEntriesResource.REQUEST_PARAM_ORGANIZATION))
               .thenReturn(ORGANIZATION_NAME);
        Mockito.when(request.getParameter(ConfigurationEntriesResource.REQUEST_PARAM_SPACE))
               .thenReturn(SPACE_NAME);
        assertEquals(new CloudTarget(ORGANIZATION_NAME, SPACE_NAME), purgeApiAuthorizationFilter.extractTarget(request));
    }

    @Test
    public void testExtractTargetWithMissingSpaceParameter() {
        Mockito.when(request.getParameter(ConfigurationEntriesResource.REQUEST_PARAM_ORGANIZATION))
               .thenReturn(ORGANIZATION_NAME);
        testExtractTargetWithMissingParameters();
    }

    @Test
    public void testExtractTargetWithMissingOrganizationParameter() {
        Mockito.when(request.getParameter(ConfigurationEntriesResource.REQUEST_PARAM_SPACE))
               .thenReturn(SPACE_NAME);
        testExtractTargetWithMissingParameters();
    }

    @Test
    public void testExtractTargetWithMissingParameters() {
        assertThrows(SLException.class, () -> purgeApiAuthorizationFilter.extractTarget(request));
    }

}
