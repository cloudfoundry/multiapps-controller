package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudStack;

public final class V3StackMapper {

    private V3StackMapper() {
    }

    public static CloudStack toCloudStack(V3Stack stack) {
        return ImmutableCloudStack.builder()
                                  .metadata(V3ResourceMappers.parseMetadata(stack.guid(), stack.createdAt(), stack.updatedAt()))
                                  .name(stack.name())
                                  .description(stack.description())
                                  .build();
    }

}
