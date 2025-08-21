package org.cloudfoundry.multiapps.controller.core.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceType;
import org.cloudfoundry.multiapps.controller.core.model.ApplicationColor;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.parser.ParametersParser;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class CloudModelBuilderUtil {

    private static final ApplicationColor COLOR_OF_APPLICATIONS_WITHOUT_SUFFIX = ApplicationColor.BLUE;

    private CloudModelBuilderUtil() {
    }

    public static Set<String> getDeployedModuleNames(List<DeployedMtaApplication> deployedApplications) {
        return deployedApplications.stream()
                                   .map(DeployedMtaApplication::getModuleName)
                                   .collect(Collectors.toCollection(TreeSet::new));
    }

    public static Set<String> getDeployedApplicationNames(List<DeployedMtaApplication> deployedApplications) {
        return deployedApplications.stream()
                                   .map(DeployedMtaApplication::getName)
                                   .collect(Collectors.toCollection(TreeSet::new));
    }

    public static boolean isService(Resource resource) {
        Set<ResourceType> resourceTypesForService = ResourceType.getServiceTypes();
        ResourceType resourceType = getResourceType(resource);
        return resourceTypesForService.contains(resourceType);
    }

    public static boolean isExistingServiceKey(Resource resource) {
        if (resource.getType() == null) {
            return false;
        }
        return ResourceType.EXISTING_SERVICE_KEY.equals(getResourceType(resource));
    }

    public static boolean isUserProvidedService(Resource resource) {
        if (resource.getType() == null) {
            return false;
        }
        return ResourceType.USER_PROVIDED_SERVICE.equals(getResourceType(resource));
    }

    public static <R> R parseParameters(List<Map<String, Object>> parametersList, ParametersParser<R> parser) {
        return parser.parse(parametersList);
    }

    public static ApplicationColor getApplicationColor(DeployedMtaApplication deployedApplication) {
        return Arrays.stream(ApplicationColor.values())
                     .filter(color -> deployedApplication.getName()
                                                         .endsWith(color.asSuffix()))
                     .findFirst()
                     .orElse(COLOR_OF_APPLICATIONS_WITHOUT_SUFFIX);
    }

    public static boolean isExistingService(List<Resource> resources, String serviceName) {
        return resources.stream()
                        .filter(resource -> ResourceType.EXISTING_SERVICE.equals(getResourceType(resource)))
                        .anyMatch(resource -> serviceName.equals(getServiceName(resource)));
    }

    public static ResourceType getResourceType(Map<String, Object> properties) {
        String type = (String) properties.getOrDefault(SupportedParameters.TYPE, ResourceType.MANAGED_SERVICE.toString());
        return ResourceType.get(type);
    }

    private static ResourceType getResourceType(Resource resource) {
        Map<String, Object> resourceParameters = resource.getParameters();
        String type = (String) resourceParameters.get(SupportedParameters.TYPE);
        return ResourceType.get(type);
    }

    private static String getServiceName(Resource resource) {
        var serviceName = (String) resource.getParameters()
                                           .get(SupportedParameters.SERVICE_NAME);
        return serviceName == null ? resource.getName() : serviceName;
    }

}
