package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class MtasApiServiceAuditLogTest {

    private static final String USERNAME = "alice";
    private static final String SPACE_ID = "space-guid";
    private static final String MTA_ID = "my-mta";
    private static final String NAMESPACE = "ns";
    private static final String NAME = "the-name";

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private MtasApiServiceAuditLog mtasApiServiceAuditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        mtasApiServiceAuditLog = new MtasApiServiceAuditLog(auditLoggingFacade);
    }

    @Test
    void testLogGetMtasWithoutFiltersEmitsDataAccess() {
        mtasApiServiceAuditLog.logGetMtas(USERNAME, SPACE_ID);

        AuditLogConfiguration captured = captureDataAccess();
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertEquals(SPACE_ID, captured.getSpaceId());
    }

    @Test
    void testLogGetMtaEmitsDataAccessWithMtaName() {
        mtasApiServiceAuditLog.logGetMta(USERNAME, SPACE_ID, MTA_ID);

        AuditLogConfiguration captured = captureDataAccess();
        Assertions.assertTrue(containsParameter(captured, "mtaName", MTA_ID));
    }

    @Test
    void testLogGetMtasWithFiltersEmitsDataAccessWithNameAndNamespace() {
        mtasApiServiceAuditLog.logGetMtas(USERNAME, SPACE_ID, NAMESPACE, NAME);

        AuditLogConfiguration captured = captureDataAccess();
        Assertions.assertTrue(containsParameter(captured, "namespace", NAMESPACE));
        Assertions.assertTrue(containsParameter(captured, "name", NAME));
    }

    private AuditLogConfiguration captureDataAccess() {
        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logDataAccessAuditLog(captor.capture());
        return captor.getValue();
    }

    private boolean containsParameter(AuditLogConfiguration configuration, String key, String value) {
        return configuration.getConfigurationIdentifiers()
                            .stream()
                            .anyMatch(identifier -> key.equals(identifier.getIdentifierName())
                                && value.equals(identifier.getIdentifierValue()));
    }

}
