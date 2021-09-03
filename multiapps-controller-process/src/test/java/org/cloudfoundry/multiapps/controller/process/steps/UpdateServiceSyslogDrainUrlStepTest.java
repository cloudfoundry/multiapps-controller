package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class UpdateServiceSyslogDrainUrlStepTest extends SyncFlowableStepTest<UpdateServiceSyslogDrainUrlStep> {

    private static final String SERVICE_NAME = "service-name";
    private static final String SYSLOG_DRAIN_URL = "test-syslog-url";

    @Test
    void testUpdateServiceSyslogDrainUrl() {
        CloudServiceInstanceExtended serviceToProcess = buildServiceToProcess();
        prepareServiceToProcess(serviceToProcess);
        prepareClient(serviceToProcess);
        step.execute(execution);
        verify(client).updateServiceSyslogDrainUrl(SERVICE_NAME, SYSLOG_DRAIN_URL);
    }

    private void prepareClient(CloudServiceInstanceExtended serviceToProcess) {
        when(client.getRequiredServiceInstanceGuid(SERVICE_NAME)).thenReturn(serviceToProcess.getGuid());
    }

    private void prepareServiceToProcess(CloudServiceInstanceExtended serviceToProcess) {
        context.setVariable(Variables.SERVICE_TO_PROCESS, serviceToProcess);
    }

    private CloudServiceInstanceExtended buildServiceToProcess() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_NAME)
                                                    .metadata(buildCloudMetadata())
                                                    .syslogDrainUrl(SYSLOG_DRAIN_URL)
                                                    .build();
    }

    private ImmutableCloudMetadata buildCloudMetadata() {
        return ImmutableCloudMetadata.builder()
                                     .guid(UUID.randomUUID())
                                     .build();
    }

    @Override
    protected UpdateServiceSyslogDrainUrlStep createStep() {
        return new UpdateServiceSyslogDrainUrlStep();
    }

}
