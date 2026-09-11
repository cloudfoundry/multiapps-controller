package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBinding.V3LastOperation;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBinding.V3RelationshipData;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBinding.V3Relationships;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBinding.V3ToOneRelationship;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3ServiceBindingMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String APP_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String INSTANCE_GUID = "99999999-8888-7777-6666-555555555555";

    @Test
    void testToCloudServiceBindingMapsAllFields() {
        V3LastOperation lastOperation = new V3LastOperation("create", "succeeded", "done", "2026-08-04T10:15:30Z",
                                                            "2026-08-04T10:16:30Z");
        V3ServiceBinding binding = new V3ServiceBinding(GUID_STRING, "my-binding", "app", null, null, lastOperation, null,
                                                        buildRelationships(APP_GUID, INSTANCE_GUID));

        CloudServiceBinding result = V3ServiceBindingMapper.toCloudServiceBinding(binding);

        Assertions.assertEquals(APP_GUID, result.getApplicationGuid()
                                                .toString());
        Assertions.assertEquals(INSTANCE_GUID, result.getServiceInstanceGuid()
                                                     .toString());
        Assertions.assertEquals(ServiceCredentialBindingOperation.Type.CREATE, result.getServiceBindingOperation()
                                                                                     .getType());
        Assertions.assertEquals(ServiceCredentialBindingOperation.State.SUCCEEDED, result.getServiceBindingOperation()
                                                                                         .getState());
    }

    @Test
    void testToCloudServiceBindingNullApplicationRelationshipReturnsNullApplicationGuid() {
        V3Relationships relationships = new V3Relationships(null, new V3ToOneRelationship(new V3RelationshipData(INSTANCE_GUID)));
        V3ServiceBinding binding = new V3ServiceBinding(GUID_STRING, "app-binding", "app", null, null, null, null, relationships);

        Assertions.assertNull(V3ServiceBindingMapper.toCloudServiceBinding(binding)
                                                    .getApplicationGuid());
    }

    @Test
    void testToCloudServiceBindingNullApplicationDataReturnsNullApplicationGuid() {
        V3Relationships relationships = new V3Relationships(new V3ToOneRelationship(null),
                                                            new V3ToOneRelationship(new V3RelationshipData(INSTANCE_GUID)));
        V3ServiceBinding binding = new V3ServiceBinding(GUID_STRING, "b", "app", null, null, null, null, relationships);

        Assertions.assertNull(V3ServiceBindingMapper.toCloudServiceBinding(binding)
                                                    .getApplicationGuid());
    }

    @Test
    void testToCloudServiceBindingNullLastOperationReturnsNullOperation() {
        V3ServiceBinding binding = new V3ServiceBinding(GUID_STRING, "b", "app", null, null, null, null,
                                                        buildRelationships(APP_GUID, INSTANCE_GUID));

        Assertions.assertNull(V3ServiceBindingMapper.toCloudServiceBinding(binding)
                                                    .getServiceBindingOperation());
    }

    private static V3Relationships buildRelationships(String appGuid, String instanceGuid) {
        return new V3Relationships(new V3ToOneRelationship(new V3RelationshipData(appGuid)),
                                   new V3ToOneRelationship(new V3RelationshipData(instanceGuid)));
    }

}
