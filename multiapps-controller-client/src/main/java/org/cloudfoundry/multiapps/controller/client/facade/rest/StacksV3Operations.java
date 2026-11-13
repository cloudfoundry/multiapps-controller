package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3QueryParameters;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Stack;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3StackMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class StacksV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Stack>> STACK_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    public StacksV3Operations(CloudControllerV3Client client) {
        this.client = client;
    }

    public CloudStack getStack(String name) {
        return getStack(name, true);
    }

    public CloudStack getStack(String name, boolean required) {
        CloudStack resultStack = findStackByName(name);

        if (resultStack == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              MessageFormat.format(Messages.STACK_0_NOT_FOUND, name));
        }

        return resultStack;
    }

    public List<CloudStack> getStacks() {
        return client.list(V3Uri.path(CloudControllerV3Endpoints.STACKS)
                                .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                                .build(), STACK_PAGE)
                     .stream()
                     .map(V3StackMapper::toCloudStack)
                     .toList();
    }

    private CloudStack findStackByName(String name) {
        return client.list(V3Uri.path(CloudControllerV3Endpoints.STACKS)
                                .query(V3QueryParameters.NAMES, name)
                                .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                                .build(), STACK_PAGE)
                     .stream()
                     .map(V3StackMapper::toCloudStack)
                     .findFirst()
                     .orElse(null);
    }

}
