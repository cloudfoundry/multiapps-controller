package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

class DetermineServiceKeysToDeleteStepTest extends SyncFlowableStepTest<DetermineServiceKeysToDeleteStep> {

    private static final String SERVICE_KEY_NAME = "the-good-key";
    private static final UUID SERVICE_INSTANCE_GUID = UUID.randomUUID();

    @Test
    void testExecuteStep() {
        context.setVariable(Variables.SERVICE_KEYS_TO_DELETE, List.of(buildDeployedMtaServiceKey()));
        step.execute(execution);
        List<CloudServiceKey> cloudServiceKeysToDelete = context.getVariable(Variables.CLOUD_SERVICE_KEYS_TO_DELETE);
        assertEquals(1, cloudServiceKeysToDelete.size());
        assertEquals(SERVICE_KEY_NAME, cloudServiceKeysToDelete.get(0)
                                                               .getName());
        assertEquals(SERVICE_INSTANCE_GUID, cloudServiceKeysToDelete.get(0)
                                                                    .getServiceInstance()
                                                                    .getGuid());
        assertStepFinishedSuccessfully();
    }

    private DeployedMtaServiceKey buildDeployedMtaServiceKey() {
        return ImmutableDeployedMtaServiceKey.builder()
                                             .name(SERVICE_KEY_NAME)
                                             .serviceInstance(buildCloudServiceInstanceExtended())
                                             .serviceKeyOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                            .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                            .state(ServiceCredentialBindingOperation.State.SUCCEEDED)
                                                                                                            .build())
                                             .build();
    }

    private CloudServiceInstanceExtended buildCloudServiceInstanceExtended() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.of(SERVICE_INSTANCE_GUID))
                                                    .build();
    }

    @Override
    protected DetermineServiceKeysToDeleteStep createStep() {
        return new DetermineServiceKeysToDeleteStep();
    }
}
