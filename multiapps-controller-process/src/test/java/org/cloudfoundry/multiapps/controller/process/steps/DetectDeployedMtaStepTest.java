package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CustomServiceKeysClient;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication.ProductizationState;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

class DetectDeployedMtaStepTest extends SyncFlowableStepTest<DetectDeployedMtaStep> {

    private static final String MTA_ID = "com.sap.xs2.samples.helloworld";
    private static final String MTA_VERSION_1 = "0.1.0";
    private static final String MTA_VERSION_2 = "0.2.0";
    private static final String DEPLOYED_MTA_LOCATION = "deployed-mta-01.json";
    private static final String DEPLOYED_MTA_SERVICE_KEYS_LOCATION = "deployed-mta-01-keys.json";

    @Mock
    private DeployedMtaDetector deployedMtaDetector;
    @Mock
    private CustomServiceKeysClient customClientMock;
    @Mock
    private TokenService tokenService;

    @BeforeEach
    public void setUp() throws Exception {
        prepareContext();
    }

    @Test
    void testExecuteWithDeployedMta() {
        DeployedMta deployedMta = JsonUtil.fromJson(TestUtil.getResourceAsString(DEPLOYED_MTA_LOCATION, getClass()), DeployedMta.class);
        List<DeployedMtaServiceKey> deployedKeys = JsonUtil.fromJson(TestUtil.getResourceAsString(DEPLOYED_MTA_SERVICE_KEYS_LOCATION,
                                                                                                  getClass()),
                                                                     TestServiceKeys.class)
                                                           .getKeys();
        List<DeployedMta> deployedComponents = List.of(deployedMta);

        when(deployedMtaDetector.detectDeployedMtas(any(CloudControllerClient.class)))
            .thenReturn(deployedComponents);
        when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(eq(MTA_ID), eq(null),
                                                                     any(CloudControllerClient.class)))
            .thenReturn(Optional.of(deployedMta));

        when(customClientMock.getServiceKeysByMetadataAndGuids(
            eq(SPACE_GUID), eq(MTA_ID), isNull(),
            eq(deployedMta.getServices()),
            argThat(List::isEmpty)))
            .thenReturn(deployedKeys);

        when(customClientMock.getServiceKeysByMetadataAndGuids(
            eq(SPACE_GUID), eq(MTA_ID), isNull(),
            anyList(),
            argThat(list -> list != null && !list.isEmpty())))
            .thenReturn(Collections.emptyList());

        step.execute(execution);

        assertStepFinishedSuccessfully();

        tester.test(() -> context.getVariable(Variables.DEPLOYED_MTA),
                    new Tester.Expectation(Tester.Expectation.Type.JSON, DEPLOYED_MTA_LOCATION));
        assertEquals(deployedKeys, context.getVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS));
    }

    @Test
    void testExecuteWithoutDeployedMta() {
        when(deployedMtaDetector.detectDeployedMtas(client)).thenReturn(Collections.emptyList());
        when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(MTA_ID, null, client)).thenReturn(Optional.empty());
        when(customClientMock.getServiceKeysByMetadataAndGuids(
            eq(SPACE_GUID), eq(MTA_ID), isNull(),
            eq(Collections.emptyList()),
            eq(Collections.emptyList())))
            .thenReturn(Collections.emptyList());

        when(customClientMock.getServiceKeysByMetadataAndGuids(
            anyString(), anyString(), isNull(),
            anyList(), anyList()))
            .thenReturn(Collections.emptyList());

        step.execute(execution);

        assertStepFinishedSuccessfully();

        assertNull(context.getVariable(Variables.DEPLOYED_MTA));
        assertEquals(Collections.emptyList(), context.getVariable(Variables.DEPLOYED_MTA_SERVICE_KEYS));
    }

    @Test
    void testExecuteWithBackupdMta() {
        DeployedMta deployedMta = ImmutableDeployedMta.builder()
                                                      .addApplication(ImmutableDeployedMtaApplication.builder()
                                                                                                     .moduleName("test-module")
                                                                                                     .name("test-app")
                                                                                                     .v3Metadata(Metadata.builder()
                                                                                                                         .annotation(
                                                                                                                             MtaMetadataAnnotations.MTA_VERSION,
                                                                                                                             MTA_VERSION_2)
                                                                                                                         .build())
                                                                                                     .productizationState(
                                                                                                         ProductizationState.LIVE)
                                                                                                     .build())
                                                      .metadata(ImmutableMtaMetadata.builder()
                                                                                    .id(MTA_ID)
                                                                                    .version(Version.parseVersion(MTA_VERSION_2))
                                                                                    .build())
                                                      .build();
        DeployedMta backupMta = ImmutableDeployedMta.builder()
                                                    .addApplication(ImmutableDeployedMtaApplication.builder()
                                                                                                   .moduleName("test-module")
                                                                                                   .name("mta-backup-test-app")
                                                                                                   .v3Metadata(Metadata.builder()
                                                                                                                       .annotation(
                                                                                                                           MtaMetadataAnnotations.MTA_VERSION,
                                                                                                                           MTA_VERSION_1)
                                                                                                                       .build())
                                                                                                   .productizationState(
                                                                                                       ProductizationState.LIVE)
                                                                                                   .build())
                                                    .metadata(ImmutableMtaMetadata.builder()
                                                                                  .id(MTA_ID)
                                                                                  .version(Version.parseVersion(MTA_VERSION_1))
                                                                                  .build())
                                                    .build();

        when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(eq(MTA_ID), eq(null),
                                                                     Mockito.any())).thenReturn(Optional.of(deployedMta));
        when(deployedMtaDetector.detectDeployedMtaByNameAndNamespace(eq(MTA_ID),
                                                                     eq(NameUtil.computeUserNamespaceWithSystemNamespace(
                                                                         Constants.MTA_BACKUP_NAMESPACE,
                                                                         null)),
                                                                     Mockito.any())).thenReturn(Optional.of(backupMta));

        step.execute(execution);

        assertStepFinishedSuccessfully();

        assertEquals(backupMta, context.getVariable(Variables.BACKUP_MTA));

    }

    private void prepareContext() {
        context.setVariable(Variables.MTA_ID, MTA_ID);
    }

    @Override
    protected DetectDeployedMtaStep createStep() {
        return new DetectDeployedMtaStepMock();
    }

    public static class TestServiceKeys {
        private List<DeployedMtaServiceKey> keys;

        public List<DeployedMtaServiceKey> getKeys() {
            return keys;
        }

        public void setKeys(List<DeployedMtaServiceKey> keys) {
            this.keys = keys;
        }
    }

    private class DetectDeployedMtaStepMock extends DetectDeployedMtaStep {
        @Override
        protected CustomServiceKeysClient getCustomServiceKeysClient(CloudCredentials credentials, String correlationId) {
            return customClientMock;
        }
    }

}
