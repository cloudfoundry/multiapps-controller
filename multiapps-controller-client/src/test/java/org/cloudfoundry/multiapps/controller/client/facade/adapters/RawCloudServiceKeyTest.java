package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.v3.LastOperation;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingRelationships;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingResource;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingType;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;

class RawCloudServiceKeyTest {

    private static final String SERVICE_NAME = "foo";
    private static final String NAME = "bar";
    private static final Map<String, Object> CREDENTIALS = buildTestCredentials();
    private static final CloudServiceInstance SERVICE_INSTANCE = ImmutableCloudServiceInstance.builder()
                                                                                              .name(SERVICE_NAME)
                                                                                              .build();
    private static final LastOperation LAST_OPERATION = buildLastOperation();

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceKey(), buildRawServiceKey());
    }

    private static CloudServiceKey buildExpectedServiceKey() {
        return ImmutableCloudServiceKey.builder()
                                       .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                       .v3Metadata(RawCloudEntityTest.V3_METADATA)
                                       .name(NAME)
                                       .credentials(CREDENTIALS)
                                       .serviceInstance(SERVICE_INSTANCE)
                                       .serviceKeyOperation(ServiceCredentialBindingOperation.from(LAST_OPERATION))
                                       .build();
    }

    private static RawCloudServiceKey buildRawServiceKey() {
        return ImmutableRawCloudServiceKey.builder()
                                          .serviceBindingResource(buildTestResource())
                                          .serviceInstance(SERVICE_INSTANCE)
                                          .credentials(CREDENTIALS)
                                          .build();
    }

    private static ServiceBindingResource buildTestResource() {
        return ServiceBindingResource.builder()
                                     .id(RawCloudEntityTest.GUID_STRING)
                                     .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                     .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                     .metadata(RawCloudEntityTest.V3_METADATA)
                                     .name(NAME)
                                     .type(ServiceBindingType.KEY)
                                     .lastOperation(LAST_OPERATION)
                                     .relationships(ServiceBindingRelationships.builder()
                                                                               .serviceInstance(RawCloudEntityTest.buildToOneRelationship(""))
                                                                               .build())
                                     .build();
    }

    private static Map<String, Object> buildTestCredentials() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("foo", "bar");
        parameters.put("baz", false);
        parameters.put("qux", 3.141);
        return parameters;
    }

    private static LastOperation buildLastOperation() {
        return LastOperation.builder()
                            .state(ServiceCredentialBindingOperation.State.SUCCEEDED.toString())
                            .type(ServiceCredentialBindingOperation.Type.CREATE.toString())
                            .createdAt(LocalDateTime.now()
                                                    .toString())
                            .updatedAt(LocalDateTime.now()
                                                    .toString())
                            .description("Service key created")
                            .build();
    }

}
