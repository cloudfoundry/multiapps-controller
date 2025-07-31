package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.time.LocalDateTime;
import java.util.UUID;

import org.cloudfoundry.client.v3.LastOperation;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingRelationships;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingResource;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingType;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;

class RawCloudServiceBindingTest {

    private static final String APPLICATION_GUID_STRING = "3725650a-8725-4401-a949-c68f83d54a86";
    private static final String SERVICE_INSTANCE_GUID_STRING = "3725650a-8725-4401-a949-c68f83d54a86";
    private static final UUID APPLICATION_GUID = UUID.fromString(APPLICATION_GUID_STRING);
    private static final UUID SERVICE_INSTANCE_GUID = UUID.fromString(SERVICE_INSTANCE_GUID_STRING);
    private static final LastOperation LAST_OPERATION = buildLastOperation();

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceBinding(), buildRawServiceBinding());
    }

    private static CloudServiceBinding buildExpectedServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
                                           .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                           .applicationGuid(APPLICATION_GUID)
                                           .serviceInstanceGuid(SERVICE_INSTANCE_GUID)
                                           .serviceBindingOperation(ServiceCredentialBindingOperation.from(LAST_OPERATION))
                                           .build();
    }

    private static RawCloudServiceBinding buildRawServiceBinding() {
        return ImmutableRawCloudServiceBinding.builder()
                                              .serviceBinding(buildTestResource())

                                              .build();
    }

    private static ServiceBindingResource buildTestResource() {
        return ServiceBindingResource.builder()
                                     .id(RawCloudEntityTest.GUID_STRING)
                                     .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                     .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                     .type(ServiceBindingType.APPLICATION)
                                     .lastOperation(LAST_OPERATION)
                                     .relationships(ServiceBindingRelationships.builder()
                                                                               .serviceInstance(RawCloudEntityTest.buildToOneRelationship(SERVICE_INSTANCE_GUID_STRING))
                                                                               .application(RawCloudEntityTest.buildToOneRelationship(APPLICATION_GUID_STRING))
                                                                               .build())
                                     .build();
    }

    private static LastOperation buildLastOperation() {
        return LastOperation.builder()
                            .state(ServiceCredentialBindingOperation.State.SUCCEEDED.toString())
                            .type(ServiceCredentialBindingOperation.Type.CREATE.toString())
                            .createdAt(LocalDateTime.now()
                                                    .toString())
                            .updatedAt(LocalDateTime.now()
                                                    .toString())
                            .description("Service binding created")
                            .build();
    }

}
