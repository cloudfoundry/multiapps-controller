package org.cloudfoundry.multiapps.controller.process.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceType;
import org.cloudfoundry.multiapps.controller.core.model.ExternalLoggingServiceConfiguration;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableExternalLoggingServiceConfiguration;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.CloudModelBuilderUtil;
import org.cloudfoundry.multiapps.controller.core.util.SpecialResourceTypesRequiredParametersUtil;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ExternalLoggingServiceConfigurationsCalculator {

    private final CloudControllerClient client;

    public ExternalLoggingServiceConfigurationsCalculator(CloudControllerClient client) {
        this.client = client;
    }

    public List<ExternalLoggingServiceConfiguration> calculateExternalLoggingServiceConfigurations(List<Resource> resources) {
        var externalLoggingServices = resources.stream()
                                               .filter(resource -> ResourceType.get(resource.getType())
                                                   == ResourceType.EXTERNAL_LOGGING_SERVICE)
                                               .toList();
        List<ExternalLoggingServiceConfiguration> externalLoggingServiceConfigurations = new ArrayList<>();
        for (Resource resource : externalLoggingServices) {
            Map<String, Object> resourceParameters = resource.getParameters();
            SpecialResourceTypesRequiredParametersUtil.checkRequiredParameters(resource.getName(), ResourceType.EXTERNAL_LOGGING_SERVICE,
                                                                               resourceParameters);
            externalLoggingServiceConfigurations.add(ImmutableExternalLoggingServiceConfiguration.builder()
                                                                                                 .serviceInstanceName(
                                                                                                     CloudModelBuilderUtil.getServiceName(
                                                                                                         resource))
                                                                                                 .serviceKeyName(
                                                                                                     (String) resourceParameters.get(
                                                                                                         SupportedParameters.SERVICE_KEY_NAME))
                                                                                                 .targetOrg((String) resourceParameters.get(
                                                                                                     SupportedParameters.ORGANIZATION_NAME))
                                                                                                 .targetSpace(
                                                                                                     (String) resourceParameters.get(
                                                                                                         SupportedParameters.SPACE_NAME))
                                                                                                 .build());
        }
        return externalLoggingServiceConfigurations;
    }

}
