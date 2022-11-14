package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;

import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

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
    void testUpdateMetadata() {
        var v3Metadata = buildGenericMetadata();
        var serviceInstanceToCreate = buildServiceInstance(v3Metadata);
        Mockito.when(processContext.getVariable(Variables.SERVICE_TO_PROCESS))
               .thenReturn(serviceInstanceToCreate);
        Mockito.when(controllerClient.getRequiredServiceInstanceGuid(SERVICE_INSTANCE_NAME))
               .thenReturn(SERVICE_INSTANCE_GUID);
        var asyncExecution = new UpdateServiceMetadataExecution();
        asyncExecution.execute(processContext);
        Mockito.verify(controllerClient)
               .updateServiceInstanceMetadata(SERVICE_INSTANCE_GUID, v3Metadata);
    }

    @Test
    void testUpdateMetadataOnOptionalService() {
        var v3Metadata = buildGenericMetadata();
        var serviceInstanceToCreate = ImmutableCloudServiceInstanceExtended.copyOf(buildServiceInstance(v3Metadata))
                                                                           .withIsOptional(true);
        Mockito.when(processContext.getVariable(Variables.SERVICE_TO_PROCESS))
               .thenReturn(serviceInstanceToCreate);
        Mockito.when(controllerClient.getRequiredServiceInstanceGuid(any()))
               .thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
        var asyncExecution = new UpdateServiceMetadataExecution();
        assertDoesNotThrow(() -> asyncExecution.execute(processContext));
    }

    private static Metadata buildGenericMetadata() {
        return Metadata.builder()
                       .annotation("a", "b")
                       .annotation("c", "d")
                       .build();
    }

    private static ImmutableCloudServiceInstanceExtended buildServiceInstance(Metadata v3Metadata) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_INSTANCE_NAME)
                                                    .plan(SERVICE_INSTANCE_PLAN)
                                                    .label(SERVICE_INSTANCE_LABEL)
                                                    .v3Metadata(v3Metadata)
                                                    .build();
    }

}
