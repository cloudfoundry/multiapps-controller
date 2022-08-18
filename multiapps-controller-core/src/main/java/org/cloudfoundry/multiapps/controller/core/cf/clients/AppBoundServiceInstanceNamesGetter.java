package org.cloudfoundry.multiapps.controller.core.cf.clients;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AppBoundServiceInstanceNamesGetter extends CustomControllerClient {

    public AppBoundServiceInstanceNamesGetter(CloudControllerClient client) {
        super(client);
    }

    public List<String> getServiceInstanceNamesBoundToApp(UUID appGuid) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> getServiceInstanceNames(appGuid));
    }

    private List<String> getServiceInstanceNames(UUID appGuid) {
        String url = "/v3/service_credential_bindings?include=service_instance&app_guids=" + appGuid.toString();
        return getListOfResources(new ServiceInstanceNamesResponseMapper(), url);
    }

    protected static class ServiceInstanceNamesResponseMapper extends ResourcesResponseMapper<String> {

        @Override
        public List<String> getMappedResources() {
            return getIncludedResources().getOrDefault("service_instances", Collections.emptyList())
                                         .stream()
                                         .map(service -> (String) ((Map<String, Object>) service).get("name"))
                                         .distinct()
                                         .collect(Collectors.toList());
        }
    }

}
