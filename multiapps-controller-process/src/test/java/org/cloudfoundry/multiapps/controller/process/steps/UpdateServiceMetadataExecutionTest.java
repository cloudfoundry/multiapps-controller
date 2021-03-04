package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

class UpdateServiceMetadataExecutionTest {

    private static final UUID SERVICE_INSTANCE_GUID = UUID.randomUUID();
    private static final String SERVICE_INSTANCE_NAME = "foo";
    private static final String SERVICE_INSTANCE_PLAN = "bar";
    private static final String SERVICE_INSTANCE_LABEL = "corge";

    @Mock
    private CloudControllerClient controllerClient;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private ProcessContext processContext;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(processContext.getControllerClient())
               .thenReturn(controllerClient);
        Mockito.when(processContext.getStepLogger())
               .thenReturn(stepLogger);
    }

    @Test
    void testExecute() {
        Metadata v3Metadata = Metadata.builder()
                                      .annotation("a", "b")
                                      .annotation("c", "d")
                                      .build();
        CloudServiceInstanceExtended serviceInstanceToCreate = ImmutableCloudServiceInstanceExtended.builder()
                                                                                                    .name(SERVICE_INSTANCE_NAME)
                                                                                                    .plan(SERVICE_INSTANCE_PLAN)
                                                                                                    .label(SERVICE_INSTANCE_LABEL)
                                                                                                    .v3Metadata(v3Metadata)
                                                                                                    .build();
        Mockito.when(processContext.getVariable(Variables.SERVICE_TO_PROCESS))
               .thenReturn(serviceInstanceToCreate);
        Mockito.when(controllerClient.getRequiredServiceInstanceGuid(SERVICE_INSTANCE_NAME))
               .thenReturn(SERVICE_INSTANCE_GUID);

        AsyncExecution asyncExecution = new UpdateServiceMetadataExecution();
        asyncExecution.execute(processContext);

        Mockito.verify(controllerClient)
               .updateServiceInstanceMetadata(SERVICE_INSTANCE_GUID, v3Metadata);
    }

}
