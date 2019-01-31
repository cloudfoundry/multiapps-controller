package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class CloudServiceNameMapper {

    private CloudModelConfiguration configuration;
    private DeploymentDescriptor deploymentDescriptor;

    public CloudServiceNameMapper(CloudModelConfiguration configuration, DeploymentDescriptor deploymentDescriptor) {
        this.configuration = configuration;
        this.deploymentDescriptor = deploymentDescriptor;
    }

    public String mapServiceName(Resource resource, ResourceType serviceType) {
        Map<String, Object> parameters = resource.getParameters();
        String overwritingName = (String) parameters.get(SupportedParameters.SERVICE_NAME);

        String shortServiceName = overwritingName != null ? overwritingName : resource.getName();
        if (serviceType.equals(ResourceType.EXISTING_SERVICE)) {
            return shortServiceName;
        }
        return getServiceName(shortServiceName);
    }

    public String getServiceName(String name) {
        return NameUtil.getServiceName(name, deploymentDescriptor.getId(), configuration.shouldUseNamespaces(),
            configuration.shouldUseNamespacesForServices());
    }
}
