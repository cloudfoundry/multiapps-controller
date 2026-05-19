package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.math.BigInteger;

import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableFileMetadata;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FilesApiServiceAuditLogTest {

    private static final String USERNAME = "alice";
    private static final String SPACE_ID = "space-guid";
    private static final String NAMESPACE = "ns";
    private static final String JOB_ID = "job-1";
    private static final String FILE_URL = "https://example.invalid/foo.mtar";

    @Mock
    private AuditLoggingFacade auditLoggingFacade;

    private FilesApiServiceAuditLog filesApiServiceAuditLog;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        filesApiServiceAuditLog = new FilesApiServiceAuditLog(auditLoggingFacade);
    }

    @Test
    void testLogGetFilesEmitsDataAccessWithNamespace() {
        filesApiServiceAuditLog.logGetFiles(USERNAME, SPACE_ID, NAMESPACE);

        AuditLogConfiguration captured = captureDataAccess();
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertEquals(SPACE_ID, captured.getSpaceId());
        Assertions.assertTrue(containsParameter(captured, "namespace", NAMESPACE));
    }

    @Test
    void testLogUploadFileEmitsConfigurationCreateWithFileMetadata() {
        FileMetadata metadata = ImmutableFileMetadata.builder()
                                                     .id("file-1")
                                                     .name("foo.mtar")
                                                     .size(BigInteger.valueOf(2048L))
                                                     .digest("abc")
                                                     .digestAlgorithm("SHA-256")
                                                     .space(SPACE_ID)
                                                     .namespace(NAMESPACE)
                                                     .build();

        filesApiServiceAuditLog.logUploadFile(USERNAME, SPACE_ID, metadata);

        AuditLogConfiguration captured = captureConfigurationChange(ConfigurationChangeActions.CONFIGURATION_CREATE);
        Assertions.assertEquals(USERNAME, captured.getUserId());
        Assertions.assertTrue(containsParameter(captured, "fileId", "file-1"));
        Assertions.assertTrue(containsParameter(captured, "digest", "abc"));
        Assertions.assertTrue(containsParameter(captured, "digestAlgorithm", "SHA-256"));
        Assertions.assertTrue(containsParameter(captured, "size", "2048"));
        Assertions.assertTrue(containsParameter(captured, "namespace", NAMESPACE));
    }

    @Test
    void testLogStartUploadFromUrlEmitsConfigurationCreateWithFileUrl() {
        filesApiServiceAuditLog.logStartUploadFromUrl(USERNAME, SPACE_ID, FILE_URL);

        AuditLogConfiguration captured = captureConfigurationChange(ConfigurationChangeActions.CONFIGURATION_CREATE);
        Assertions.assertTrue(containsParameter(captured, "fileUrl", FILE_URL));
    }

    @Test
    void testLogGetUploadFromUrlJobEmitsDataAccessWithNamespaceAndJobId() {
        filesApiServiceAuditLog.logGetUploadFromUrlJob(USERNAME, SPACE_ID, NAMESPACE, JOB_ID);

        AuditLogConfiguration captured = captureDataAccess();
        Assertions.assertTrue(containsParameter(captured, "namespace", NAMESPACE));
        Assertions.assertTrue(containsParameter(captured, "jobId", JOB_ID));
    }

    private AuditLogConfiguration captureDataAccess() {
        ArgumentCaptor<AuditLogConfiguration> captor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        Mockito.verify(auditLoggingFacade)
               .logDataAccessAuditLog(captor.capture());
        return captor.getValue();
    }

    private AuditLogConfiguration captureConfigurationChange(ConfigurationChangeActions expectedAction) {
        ArgumentCaptor<AuditLogConfiguration> configCaptor = ArgumentCaptor.forClass(AuditLogConfiguration.class);
        ArgumentCaptor<ConfigurationChangeActions> actionCaptor = ArgumentCaptor.forClass(ConfigurationChangeActions.class);
        Mockito.verify(auditLoggingFacade)
               .logConfigurationChangeAuditLog(configCaptor.capture(), actionCaptor.capture());
        Assertions.assertEquals(expectedAction, actionCaptor.getValue());
        return configCaptor.getValue();
    }

    private boolean containsParameter(AuditLogConfiguration configuration, String key, String value) {
        return configuration.getConfigurationIdentifiers()
                            .stream()
                            .anyMatch(identifier -> key.equals(identifier.getIdentifierName())
                                && value.equals(identifier.getIdentifierValue()));
    }

}
