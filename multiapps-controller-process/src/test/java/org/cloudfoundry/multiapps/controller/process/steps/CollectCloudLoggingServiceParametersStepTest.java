package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.auditlogging.CloudLoggingServiceConfigurationAuditLog;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging.CloudLoggingServiceConfigurationService;
import org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging.UnsentProcessLogsProvider;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CollectCloudLoggingServiceParametersStepTest extends SyncFlowableStepTest<CollectCloudLoggingServiceParametersStepTest.TestableStep> {

    private static final String MTA_ID = "test-mta";
    private static final String NAMESPACE = "ns-1";
    private static final String CONFIG_ID = "config-id-1";
    private static final String CLOUD_LOGGING_RESOURCE_TYPE = "org.cloudfoundry.cloud-logging-service";

    private TokenService tokenService;
    private CloudControllerClientFactory clientFactory;
    private CloudLoggingServiceConfigurationService configurationService;
    private ProcessTypeParser processTypeParser;
    private CloudLoggingServiceConfigurationAuditLog auditLog;
    private UnsentProcessLogsProvider unsentProcessLogsProvider;

    static class TestableStep extends CollectCloudLoggingServiceParametersStep {

        LoggingConfiguration nextBuiltFromDescriptor;
        LoggingConfiguration nextBuiltFromExisting;

        TestableStep(TokenService tokenService, CloudControllerClientFactory clientFactory,
                     CloudLoggingServiceConfigurationService configurationService, ProcessTypeParser processTypeParser,
                     CloudLoggingServiceConfigurationAuditLog auditLog, UnsentProcessLogsProvider unsentProcessLogsProvider) {
            super(tokenService, clientFactory, configurationService, processTypeParser, auditLog, unsentProcessLogsProvider);
        }

        @Override
        protected LoggingConfiguration setExternalLoggingServiceConfigurationIfRequired(ProcessContext context,
                                                                                        DeploymentDescriptor deploymentDescriptor) {
            return nextBuiltFromDescriptor;
        }

        @Override
        protected LoggingConfiguration setExternalLoggingServiceConfigurationIfRequired(ProcessContext context,
                                                                                        LoggingConfiguration loggingConfiguration) {
            return nextBuiltFromExisting == null ? loggingConfiguration : nextBuiltFromExisting;
        }
    }

    @BeforeEach
    void prepareContext() {
        context.setVariable(Variables.MTA_ID, MTA_ID);
        context.setVariable(Variables.MTA_NAMESPACE, NAMESPACE);
    }

    @Override
    protected TestableStep createStep() {
        tokenService = Mockito.mock(TokenService.class);
        clientFactory = Mockito.mock(CloudControllerClientFactory.class);
        configurationService = Mockito.mock(CloudLoggingServiceConfigurationService.class);
        processTypeParser = Mockito.mock(ProcessTypeParser.class);
        auditLog = Mockito.mock(CloudLoggingServiceConfigurationAuditLog.class);
        unsentProcessLogsProvider = Mockito.mock(UnsentProcessLogsProvider.class);
        return new TestableStep(tokenService, clientFactory, configurationService, processTypeParser, auditLog,
                                unsentProcessLogsProvider);
    }

    @Test
    void undeploy_noExistingConfig_finishesWithoutSettingVariable() {
        when(processTypeParser.getProcessType(any())).thenReturn(ProcessType.UNDEPLOY);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertNull(context.getVariable(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION));
        verify(auditLog).logGetLoggingConfiguration(any(), any(), any());
        verify(unsentProcessLogsProvider, never()).getUnsentProcessLogs(any());
    }

    @Test
    void undeploy_existingConfig_setsVariableAndAuditsGet() throws FileStorageException {
        LoggingConfiguration existing = buildConfig();
        when(processTypeParser.getProcessType(any())).thenReturn(ProcessType.UNDEPLOY);
        when(configurationService.getLoggingConfiguration(any(), any(), any())).thenReturn(existing);
        when(unsentProcessLogsProvider.getUnsentProcessLogs(existing)).thenReturn(List.of());

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(existing, context.getVariable(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION));
        verify(auditLog).logGetLoggingConfiguration(USER_NAME, SPACE_GUID, existing);
        verify(configurationService, never()).add(any());
        verify(configurationService, never()).update(any(), any());
        verify(configurationService, never()).deleteLoggingConfiguration(any());
    }

    @Test
    void deploy_noCloudLoggingResource_noExistingConfig_finishesWithoutSideEffects() {
        prepareDeployContext(descriptorWithoutCloudLogging());

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertNull(context.getVariable(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION));
        verify(configurationService, never()).deleteLoggingConfiguration(any());
        verify(auditLog, never()).logDeleteLoggingConfiguration(any(), any(), any());
        verify(unsentProcessLogsProvider, never()).getUnsentProcessLogs(any());
    }

    @Test
    void deploy_noCloudLoggingResource_existingConfig_deletesAndAudits() {
        LoggingConfiguration existing = buildConfig();
        prepareDeployContext(descriptorWithoutCloudLogging());
        when(configurationService.getLoggingConfiguration(any(), any(), any())).thenReturn(existing);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertNull(context.getVariable(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION));
        verify(auditLog).logDeleteLoggingConfiguration(USER_NAME, SPACE_GUID, existing);
        verify(configurationService).deleteLoggingConfiguration(existing.getId());
        verify(unsentProcessLogsProvider, never()).getUnsentProcessLogs(any());
    }

    @Test
    void deploy_cloudLoggingResource_builderReturnsNull_finishesWithoutPersistingOrSettingVariable() {
        prepareDeployContext(descriptorWithCloudLogging());
        step.nextBuiltFromDescriptor = null;

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertNull(context.getVariable(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION));
        verify(configurationService, never()).add(any());
        verify(configurationService, never()).update(any(), any());
        verify(unsentProcessLogsProvider, never()).getUnsentProcessLogs(any());
    }

    @Test
    void deploy_cloudLoggingResource_noExistingConfig_storesAndAuditsCreate() throws FileStorageException {
        LoggingConfiguration newConfig = buildConfig();
        prepareDeployContext(descriptorWithCloudLogging());
        step.nextBuiltFromDescriptor = newConfig;
        when(unsentProcessLogsProvider.getUnsentProcessLogs(newConfig)).thenReturn(List.of());

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(newConfig, context.getVariable(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION));
        verify(auditLog).logCreateLoggingConfiguration(USER_NAME, SPACE_GUID, newConfig);
        verify(configurationService).add(newConfig);
        verify(configurationService, never()).update(any(), any());
    }

    @Test
    void deploy_cloudLoggingResource_existingConfig_updatesAndAuditsUpdateAndGet() throws FileStorageException {
        LoggingConfiguration existing = buildConfig();
        LoggingConfiguration rebuilt = ImmutableLoggingConfiguration.builder()
                                                                    .from(existing)
                                                                    .logLevel(LogLevel.ERROR)
                                                                    .build();
        prepareDeployContext(descriptorWithCloudLogging());
        when(configurationService.getLoggingConfiguration(any(), any(), any())).thenReturn(existing);
        step.nextBuiltFromDescriptor = rebuilt;
        when(unsentProcessLogsProvider.getUnsentProcessLogs(rebuilt)).thenReturn(List.of());

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(rebuilt, context.getVariable(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION));
        verify(auditLog).logGetLoggingConfiguration(USER_NAME, SPACE_GUID, existing);
        verify(auditLog).logUpdateLoggingConfiguration(USER_NAME, SPACE_GUID, rebuilt);
        verify(configurationService).update(existing, rebuilt);
        verify(configurationService, never()).add(any());
    }

    @Test
    void deploy_cloudLoggingResource_forwardsEveryUnsentEntry() throws FileStorageException {
        LoggingConfiguration newConfig = buildConfig();
        OperationLogEntry entry1 = ImmutableOperationLogEntry.builder()
                                                             .operationId("op-1")
                                                             .operationLog("log-1")
                                                             .operationLogName("svc-1")
                                                             .build();
        OperationLogEntry entry2 = ImmutableOperationLogEntry.builder()
                                                             .operationId("op-2")
                                                             .operationLog("log-2")
                                                             .operationLogName("svc-2")
                                                             .build();
        prepareDeployContext(descriptorWithCloudLogging());
        step.nextBuiltFromDescriptor = newConfig;
        when(unsentProcessLogsProvider.getUnsentProcessLogs(newConfig)).thenReturn(List.of(entry1, entry2));

        step.execute(execution);

        assertStepFinishedSuccessfully();
        verify(operationLogsExporter).sendLogsToCloudLoggingService(newConfig, entry1);
        verify(operationLogsExporter).sendLogsToCloudLoggingService(newConfig, entry2);
    }

    @Test
    void deploy_cloudLoggingResource_noUnsentEntries_doesNotForward() throws FileStorageException {
        LoggingConfiguration newConfig = buildConfig();
        prepareDeployContext(descriptorWithCloudLogging());
        step.nextBuiltFromDescriptor = newConfig;
        when(unsentProcessLogsProvider.getUnsentProcessLogs(newConfig)).thenReturn(List.of());

        step.execute(execution);

        assertStepFinishedSuccessfully();
        verify(operationLogsExporter, never()).sendLogsToCloudLoggingService(any(), Mockito.<OperationLogEntry> any());
    }

    private void prepareDeployContext(DeploymentDescriptor descriptor) {
        when(processTypeParser.getProcessType(any())).thenReturn(ProcessType.DEPLOY);
        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR, descriptor);
    }

    private static DeploymentDescriptor descriptorWithCloudLogging() {
        return DeploymentDescriptor.createV3()
                                   .setResources(List.of(Resource.createV3()
                                                                 .setName("my-cls")
                                                                 .setType(CLOUD_LOGGING_RESOURCE_TYPE)));
    }

    private static DeploymentDescriptor descriptorWithoutCloudLogging() {
        return DeploymentDescriptor.createV3()
                                   .setResources(List.of(Resource.createV3()
                                                                 .setName("not-cls")
                                                                 .setType("org.cloudfoundry.managed-service")));
    }

    private static LoggingConfiguration buildConfig() {
        return ImmutableLoggingConfiguration.builder()
                                            .id(CONFIG_ID)
                                            .mtaId(MTA_ID)
                                            .mtaSpaceId(SPACE_NAME)
                                            .namespace(NAMESPACE)
                                            .logLevel(LogLevel.INFO)
                                            .isFailSafe(true)
                                            .build();
    }
}
