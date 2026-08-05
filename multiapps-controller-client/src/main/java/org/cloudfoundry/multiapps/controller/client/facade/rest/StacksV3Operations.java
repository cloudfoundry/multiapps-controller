package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudStack;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Stack;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3StackMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

/**
 * CF v3 <em>stacks</em> operations of the cf-java-client replacement. Reproduces the HTTP shape and domain mapping of the OSS
 * {@code CloudControllerRestClientImpl} stack methods:
 * <ul>
 * <li>{@code getStacks()} &rarr; {@code GET /v3/stacks} (paginated);</li>
 * <li>{@code getStack(name[, required])} &rarr; {@code GET /v3/stacks?names=<name>}, taking the first match.</li>
 * </ul>
 * Stacks are a global resource, so — like the OSS impl — the listing is not filtered by the target space.
 */
public class StacksV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Stack>> STACK_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public StacksV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public CloudStack getStack(String name) {
        return getStack(name, true);
    }

    public CloudStack getStack(String name, boolean required) {
        CloudStack stack = findStackByName(name);
        if (stack == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Stack " + name + " not found.");
        }
        return stack;
    }

    public List<CloudStack> getStacks() {
        return cc.list("/v3/stacks?per_page=5000", STACK_PAGE)
                 .stream()
                 .map(V3StackMapper::toCloudStack)
                 .toList();
    }

    private CloudStack findStackByName(String name) {
        return cc.list("/v3/stacks?names=" + name + "&per_page=5000", STACK_PAGE)
                 .stream()
                 .map(V3StackMapper::toCloudStack)
                 .findFirst()
                 .orElse(null);
    }

}
