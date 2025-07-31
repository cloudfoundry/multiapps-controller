package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.stacks.Stack;
import org.cloudfoundry.client.v3.stacks.StackResource;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudStack;

class RawCloudStackTest {

    private static final String NAME = "cflinuxfs3";

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedStack(), buildRawStack());
    }

    private static CloudStack buildExpectedStack() {
        return ImmutableCloudStack.builder()
                                  .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                  .name(NAME)
                                  .build();
    }

    private static RawCloudStack buildRawStack() {
        return ImmutableRawCloudStack.of(buildTestResource());
    }

    private static Stack buildTestResource() {
        return StackResource.builder()
                            .id(RawCloudEntityTest.GUID_STRING)
                            .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                            .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                            .metadata(RawCloudEntityTest.V3_METADATA)
                            .name(NAME)
                            .build();
    }

}
