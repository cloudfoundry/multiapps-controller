package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Stack;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3StackMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class StacksV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Stack>> STACK_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;

    public StacksV3Operations(CloudControllerV3Client cc) {
        this.cc = cc;
    }

    public CloudStack getStack(String name) {
        return getStack(name, true);
    }

    public CloudStack getStack(String name, boolean required) {
        CloudStack resultStack = findStackByName(name);

        if (resultStack == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.STACK_0_NOT_FOUND, name));
        }

        return resultStack;
    }

    public List<CloudStack> getStacks() {
        return cc.list(CloudControllerV3Endpoints.STACKS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                           + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, STACK_PAGE)
                 .stream()
                 .map(V3StackMapper::toCloudStack)
                 .toList();
    }

    private CloudStack findStackByName(String name) {
        return cc.list(CloudControllerV3Endpoints.STACKS + CloudControllerV3Endpoints.QUERY_NAMES + name
                           + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, STACK_PAGE)
                 .stream()
                 .map(V3StackMapper::toCloudStack)
                 .findFirst()
                 .orElse(null);
    }

}
