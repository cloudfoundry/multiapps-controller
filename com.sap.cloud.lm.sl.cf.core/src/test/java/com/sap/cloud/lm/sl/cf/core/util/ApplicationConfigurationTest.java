package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingFacade;
import com.sap.cloud.lm.sl.cf.core.helpers.Environment;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration.DatabaseType;

import static org.mockito.Mockito.when;

import jersey.repackaged.com.google.common.collect.ImmutableMap;

public class ApplicationConfigurationTest {

    // TODO - tests about masking of sensitive entries while logging
    // TODO - tests about parsing values of different types
    // TODO - tests about taking the default when the value is incorrect

    @Mock
    private Environment environmentMock;
    @Mock
    private AuditLoggingFacade auditLoggingFascade;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testLoadDefaultsNoGivenValues() {
        ApplicationConfiguration testedConfig = new ApplicationConfiguration(environmentMock);
        testedConfig.load();
        assertEquals(DatabaseType.DEFAULTDB, testedConfig.getDatabaseType());
        
    }

    @Test
    public void testReadDBType() {
        DatabaseType expectedDBType = DatabaseType.POSTGRESQL;
        when(environmentMock.getVariable(ApplicationConfiguration.CFG_DB_TYPE)).thenReturn(expectedDBType.toString());
        ApplicationConfiguration testedConfiguration = new ApplicationConfiguration(environmentMock);
        assertEquals(expectedDBType, testedConfiguration.getDatabaseType());
    }

    @Test
    public void testLogFullConfig() {
        when(environmentMock.getVariables()).thenReturn(ImmutableMap.of(ApplicationConfiguration.CFG_DB_TYPE, "POSTGRES"));
        ApplicationConfiguration testedConfig = new ApplicationConfiguration(environmentMock) {
            protected AuditLoggingFacade getAuditLoggingFascade() {
                return auditLoggingFascade;
            }
        };
        testedConfig.logFullConfig();
    }

}
