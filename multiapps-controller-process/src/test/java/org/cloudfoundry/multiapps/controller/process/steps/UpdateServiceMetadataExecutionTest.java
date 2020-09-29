package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Date;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
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

class UpdateServiceMetadataExecutionTest {

    private static final String SERVICE_INSTANCE_NAME = "foo";
    private static final String SERVICE_INSTANCE_PLAN = "bar";
    private static final String SERVICE_INSTANCE_LABEL = "corge";

    @Mock
    private CloudControllerClient controllerClient;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private ProcessContext procesContext;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(procesContext.getControllerClient())
               .thenReturn(controllerClient);
        Mockito.when(procesContext.getStepLogger())
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
        Mockito.when(procesContext.getVariable(Variables.SERVICE_TO_PROCESS))
               .thenReturn(serviceInstanceToCreate);

        CloudMetadata metadata = ImmutableCloudMetadata.builder()
                                                       .guid(UUID.randomUUID())
                                                       .createdAt(new Date())
                                                       .updatedAt(new Date())
                                                       .build();
        CloudServiceInstance serviceInstance = ImmutableCloudServiceInstance.builder()
                                                                            .name(SERVICE_INSTANCE_NAME)
                                                                            .plan(SERVICE_INSTANCE_PLAN)
                                                                            .label(SERVICE_INSTANCE_LABEL)
                                                                            .metadata(metadata)
                                                                            .build();
        Mockito.when(controllerClient.getServiceInstance(serviceInstance.getName()))
               .thenReturn(serviceInstance);

        AsyncExecution asyncExecution = new UpdateServiceMetadataExecution();
        asyncExecution.execute(procesContext);

        Mockito.verify(controllerClient)
               .updateServiceInstanceMetadata(metadata.getGuid(), v3Metadata);
    }

}
