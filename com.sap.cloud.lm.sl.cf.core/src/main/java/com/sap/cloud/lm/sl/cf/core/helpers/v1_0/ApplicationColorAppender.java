package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.HdiResourceTypeEnum;
import com.sap.cloud.lm.sl.cf.core.model.ModuleTypeEnum;
import com.sap.cloud.lm.sl.cf.core.model.ResourceTypeEnum;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.model.ZdmActionEnum;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.Visitor;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource.ResourceBuilder;

public class ApplicationColorAppender extends Visitor {

    private static final String JBP_CONFIG_RESOURCE_CONFIGURATION = "JBP_CONFIG_RESOURCE_CONFIGURATION";
    protected static final String RESOURCE_IS_NOT_FOUND_ERROR = "Resource \"{0}\" is not found";
    private static final String NOT_ONLY_ONE_BOUND_RESOURCE_ERROR = "Module \"{0}\" does not have only one bounded resource.";

    private static final String ZDM_ENV_VAR_TARGET_CONTAINER = "TARGET_CONTAINER";
    private static final String ZDM_ENV_VAR_MODE = "HDI_DEPLOY_MODE";
    private static final String ZDM_ENV_VAR_MODE_ZDM = "ZDM";
    private static final String ZDM_ENV_VAR_SERVICE_REPLACEMENTS = "SERVICE_REPLACEMENTS";
    private static final String ZDM_ENV_VAR_SERVICE_REPLACEMENTS_KEY = "key";
    private static final String ZDM_ENV_VAR_SERVICE_REPLACEMENTS_SERVICE = "service";
    private static final String ZDM_ENV_VAR_SERVICE_REPLACEMENTS_DATA_SERVICE = "zdm-data-service";
    private static final String ZDM_ENV_VAR_SERVICE_REPLACEMENTS_ACCESS_SERVICE = "zdm-access-service";
    private static final String ZDM_ENV_VAR_SERVICE_REPLACEMENTS_TEMP_SERVICE = "zdm-temp-service";
    private static final List<String> ZDM_LOGICAL_SERVICES = Arrays.asList(ZDM_ENV_VAR_SERVICE_REPLACEMENTS_DATA_SERVICE,
        ZDM_ENV_VAR_SERVICE_REPLACEMENTS_ACCESS_SERVICE, ZDM_ENV_VAR_SERVICE_REPLACEMENTS_TEMP_SERVICE);
    private static final String OVERRIDE_SERVICE_REPLACEMENTS_ERROR = "\"SERVICE_REPLACEMENTS\" key \"{0}\" is reserved and should not be defined. Reserved keys: "
        + ZDM_LOGICAL_SERVICES + ".";

    protected ApplicationColor applicationColor;
    protected ApplicationColor deployedMtaColor;
    protected String zdmAction;
    protected Map<String, Map<String, String>> moduleNameToZdmResourcesMap;

    public ApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        this.applicationColor = applicationColor;
        this.deployedMtaColor = deployedMtaColor;
        this.moduleNameToZdmResourcesMap = new TreeMap<>();
    }

    @Override
    public void visit(ElementContext context, Module module) {
        Map<String, Object> properties = new TreeMap<>(module.getProperties());
        properties.put(SupportedParameters.APP_NAME, computeAppName(module));
        module.setProperties(properties);
    }

    protected String computeAppName(Module module) {
        Map<String, Object> moduleProperties = module.getProperties();
        String moduleName = module.getName();
        return this.getAppName(moduleProperties, moduleName);
    }

    protected String getAppName(Map<String, Object> moduleProperties, String moduleName) {
        return (String) moduleProperties.getOrDefault(SupportedParameters.APP_NAME, moduleName) + applicationColor.asSuffix();
    }

    @Override
    public void visit(ElementContext context, DeploymentDescriptor descriptor) {
        ZdmHelper zdmHelper = new ZdmHelper();
        if (!zdmHelper.existsZdmMarker(descriptor, this.getMajorSchemaVersion(), this.getMinorSchemaVersion())) {
            return;
        }
        changeHdiDeployerModulesToZdmType(descriptor);
        setZdmAction();
        HandlerFactory handlerFactory = new HandlerFactory(this.getMajorSchemaVersion(), this.getMinorSchemaVersion());
        DescriptorHandler descriptorHandler = handlerFactory.getDescriptorHandler();
        handleDescriptor(descriptor, descriptorHandler);
    }

    private void changeHdiDeployerModulesToZdmType(DeploymentDescriptor descriptor) {
        HandlerFactory handlerFactory = new HandlerFactory(this.getMajorSchemaVersion(), this.getMinorSchemaVersion());
        PropertiesAccessor pA = handlerFactory.getPropertiesAccessor();
        for (Module module : descriptor.getModules1_0()) {
            Map<String, Object> parameters = pA.getParameters(module);
            if (parameters.containsKey(SupportedParameters.ZDM_MODE.toString())
                && module.getType().equals(com.sap.cloud.lm.sl.cf.core.model.ModuleTypeEnum.HDI.toString())) {
                module.setType(com.sap.cloud.lm.sl.cf.core.model.ModuleTypeEnum.HDI_ZDM.toString());
            }
        }
    }

    private void setZdmAction() {
        this.zdmAction = ZdmActionEnum.INSTALL.toString();
        if (this.deployedMtaColor != null) {
            this.zdmAction = ZdmActionEnum.START.toString();
        }
    }

    private void handleDescriptor(DeploymentDescriptor descriptor, DescriptorHandler descriptorHandler) {
        handleHdiModules(descriptor, descriptorHandler);
        bindModulesToNewAccessContainers(descriptor);
    }

    private void handleHdiModules(DeploymentDescriptor descriptor, DescriptorHandler descriptorHandler) {
        for (Module module : descriptor.getModules1_0()) {
            if (!module.getType().equals(ModuleTypeEnum.HDI_ZDM.toString())) {
                continue;
            }
            handleHdiModule(descriptor, descriptorHandler, module);
        }
    }

    protected void handleHdiModule(DeploymentDescriptor descriptor, DescriptorHandler descriptorHandler, Module module) {
        List<String> nonHdiRequiredDependencies = new ArrayList<>();
        List<Resource> requiredHdiResources = new ArrayList<>();
        List<String> requiredDependencies = module.getRequiredDependencies1_0();

        for (String dependencyName : requiredDependencies) {
            Resource resource = (Resource) descriptorHandler.findResource(descriptor, dependencyName);
            if (resource == null) {
                throw new SLException(format(RESOURCE_IS_NOT_FOUND_ERROR, dependencyName));
            }
            if (resource.getType().equals(ResourceTypeEnum.HDI_CONTAINER.toString())) {
                requiredHdiResources.add(resource);
                continue;
            }
            nonHdiRequiredDependencies.add(dependencyName);
        }

        addZdmResources(descriptor, module, requiredHdiResources, nonHdiRequiredDependencies);
        addZdmProperties(module);
    }

    @SuppressWarnings("unchecked")
    private void addZdmResources(DeploymentDescriptor descriptor, Module module, List<Resource> requiredHdiResources,
        List<String> nonHdiRequiredDependencies) {

        if (requiredHdiResources.size() != 1) {
            String appName = module.getName();
            throw new SLException(format(NOT_ONLY_ONE_BOUND_RESOURCE_ERROR, appName));
        }

        List<Resource> newRequiredHdiResources = (List<Resource>) createNewHdiResources(module, requiredHdiResources);
        requiredHdiResources.addAll(newRequiredHdiResources);

        List<Resource> descriptorResources = new ArrayList<>(descriptor.getResources1_0());
        for (Resource newHdiResource : newRequiredHdiResources) {
            descriptorResources.add(newHdiResource);
        }
        descriptor.setResources1_0(descriptorResources);

        List<String> allRequiredDependencies = new ArrayList<>();
        allRequiredDependencies.addAll(nonHdiRequiredDependencies);
        for (Resource hdiResource : requiredHdiResources) {
            allRequiredDependencies.add(hdiResource.getName());
        }
        module.setRequiredDependencies1_0(allRequiredDependencies);
    }

    protected List<? extends Resource> createNewHdiResources(Module module, List<? extends Resource> requiredHdiResources) {
        List<Resource> newRequiredHdiResources = new ArrayList<>();
        String accessContainerSuffix = HdiResourceTypeEnum.ACCESS.asSuffix() + this.applicationColor.asSuffix();

        Resource dataContainer = requiredHdiResources.get(0);
        String dataContainerName = dataContainer.getName();

        String accessContainerName = dataContainerName + accessContainerSuffix;
        Resource accessContainer = (Resource) buildResource(accessContainerName, ResourceTypeEnum.HDI_CONTAINER.toString());
        newRequiredHdiResources.add(accessContainer);

        String tempContainerSuffix = HdiResourceTypeEnum.TEMP.asSuffix();
        String tempContainerName = dataContainerName + tempContainerSuffix;
        Resource tempContainer = (Resource) buildResource(tempContainerName, ResourceTypeEnum.HDI_CONTAINER.toString());
        newRequiredHdiResources.add(tempContainer);

        Map<String, String> moduleMap = new TreeMap<>();
        moduleMap.put(HdiResourceTypeEnum.DATA.toString(), dataContainerName);
        moduleMap.put(HdiResourceTypeEnum.ACCESS.toString(), accessContainerName);
        moduleMap.put(HdiResourceTypeEnum.TEMP.toString(), tempContainerName);
        moduleNameToZdmResourcesMap.put(computeAppName(module), moduleMap);

        return newRequiredHdiResources;
    }

    protected Resource buildResource(String resourceName, String resourceType) {
        ResourceBuilder builder = getResourceBuilder();
        builder.setName(resourceName);
        builder.setType(resourceType);
        return builder.build();
    }

    protected ResourceBuilder getResourceBuilder() {
        return new ResourceBuilder();
    }

    protected void addZdmProperties(Module module) {
        Map<String, String> moduleResourcesMap = moduleNameToZdmResourcesMap.get(computeAppName(module));

        String dataContainerName = moduleResourcesMap.get(HdiResourceTypeEnum.DATA.toString());
        String accessContainerName = moduleResourcesMap.get(HdiResourceTypeEnum.ACCESS.toString());
        String tempContainerName = moduleResourcesMap.get(HdiResourceTypeEnum.TEMP.toString());

        Map<String, Object> moduleProperties = new TreeMap<>(module.getProperties());
        moduleProperties.put(ZDM_ENV_VAR_TARGET_CONTAINER, dataContainerName);
        moduleProperties.put(ZDM_ENV_VAR_MODE, ZDM_ENV_VAR_MODE_ZDM);
        moduleProperties.put(ZdmActionEnum.ZDM_ACTION.toString(), this.zdmAction);

        String serviceReplacementsJson = createServiceReplacementsJson(dataContainerName, accessContainerName, tempContainerName,
            moduleProperties);
        moduleProperties.put(ZDM_ENV_VAR_SERVICE_REPLACEMENTS, serviceReplacementsJson);

        module.setProperties(moduleProperties);
    }

    @SuppressWarnings("unchecked")
    private String createServiceReplacementsJson(String dataContainerName, String accessContainerName, String tempContainerName,
        Map<String, Object> moduleProperties) {
        Object moduleServiceReplacementsObject = moduleProperties.get(ZDM_ENV_VAR_SERVICE_REPLACEMENTS);
        List<Map<String, String>> moduleServiceReplacements = new ArrayList<>();

        if (moduleServiceReplacementsObject instanceof String) {
            moduleServiceReplacements = CommonUtil.cast(JsonUtil.convertJsonToList((String) moduleServiceReplacementsObject));
        } else if (moduleServiceReplacementsObject instanceof List) {
            moduleServiceReplacements = (List<Map<String, String>>) moduleServiceReplacementsObject;
        }

        List<Map<String, String>> zdmServiceReplacements = createZdmServiceReplacements(dataContainerName, accessContainerName,
            tempContainerName);
        mergeServiceReplacements(moduleServiceReplacements, zdmServiceReplacements);

        return JsonUtil.toJson(moduleServiceReplacements);
    }

    private void mergeServiceReplacements(List<Map<String, String>> moduleServiceReplacements,
        List<Map<String, String>> zdmServiceReplacements) {
        for (Map<String, String> originalKeyServiceMap : moduleServiceReplacements) {
            String logicalServiceName = originalKeyServiceMap.get("key");
            if (ZDM_LOGICAL_SERVICES.contains(logicalServiceName)) {
                throw new ContentException(format(OVERRIDE_SERVICE_REPLACEMENTS_ERROR, logicalServiceName));
            }
        }

        moduleServiceReplacements.addAll(zdmServiceReplacements);
    }

    private List<Map<String, String>> createZdmServiceReplacements(String dataContainerName, String accessContainerName,
        String tempContainerName) {
        Map<String, String> dataContainerMap = new TreeMap<>();
        dataContainerMap.put(ZDM_ENV_VAR_SERVICE_REPLACEMENTS_KEY, ZDM_ENV_VAR_SERVICE_REPLACEMENTS_DATA_SERVICE);
        dataContainerMap.put(ZDM_ENV_VAR_SERVICE_REPLACEMENTS_SERVICE, dataContainerName);

        Map<String, String> accessContainerMap = new TreeMap<>();
        accessContainerMap.put(ZDM_ENV_VAR_SERVICE_REPLACEMENTS_KEY, ZDM_ENV_VAR_SERVICE_REPLACEMENTS_ACCESS_SERVICE);
        accessContainerMap.put(ZDM_ENV_VAR_SERVICE_REPLACEMENTS_SERVICE, accessContainerName);

        Map<String, String> tempContainerMap = new TreeMap<>();
        tempContainerMap.put(ZDM_ENV_VAR_SERVICE_REPLACEMENTS_KEY, ZDM_ENV_VAR_SERVICE_REPLACEMENTS_TEMP_SERVICE);
        tempContainerMap.put(ZDM_ENV_VAR_SERVICE_REPLACEMENTS_SERVICE, tempContainerName);

        List<Map<String, String>> serviceReplacements = new ArrayList<>();
        serviceReplacements.add(dataContainerMap);
        serviceReplacements.add(accessContainerMap);
        serviceReplacements.add(tempContainerMap);

        return serviceReplacements;
    }

    private void bindModulesToNewAccessContainers(DeploymentDescriptor descriptor) {
        for (Module module : descriptor.getModules1_0()) {
            if (module.getType().equals(ModuleTypeEnum.HDI_ZDM.toString()) || module.getType().equals(ModuleTypeEnum.HDI.toString())) {
                continue;
            }
            bindModuleToNewAccessContainer(module);
            setNewAccessContainerInResourceConfigurationProperty(module);
        }
    }

    protected void bindModuleToNewAccessContainer(Module module) {
        List<String> requiredDependencies = module.getRequiredDependencies1_0();
        List<String> newRequiredDependencies = new ArrayList<>();

        for (String dependencyName : requiredDependencies) {
            if (!isDataContainer(dependencyName)) {
                newRequiredDependencies.add(dependencyName);
                continue;
            }
            newRequiredDependencies.add(getCorrespondingAccessContainer(dependencyName));
        }

        module.setRequiredDependencies1_0(newRequiredDependencies);
    }

    // Specific configuration property for mapping of data source to resource in SAP Java buildpack
    private void setNewAccessContainerInResourceConfigurationProperty(Module module) {
        Map<String, Object> properties = module.getProperties();
        Object sapJbpResourceConfigurationProperty = properties.get(JBP_CONFIG_RESOURCE_CONFIGURATION);

        if (sapJbpResourceConfigurationProperty == null) {
            return;
        }

        String sapJbpResourceConfiguration = (String) sapJbpResourceConfigurationProperty;
        for (String dataContainerName : getDataContainerNames()) {
            if (!sapJbpResourceConfiguration.contains(dataContainerName)) {
                continue;
            }
            String regex = "\\b" + dataContainerName + "\\b";
            sapJbpResourceConfiguration = sapJbpResourceConfiguration.replaceAll(regex, getCorrespondingAccessContainer(dataContainerName));
        }

        Map<String, Object> newProperties = new TreeMap<>();
        for (Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getKey().equals(JBP_CONFIG_RESOURCE_CONFIGURATION)) {
                newProperties.put(JBP_CONFIG_RESOURCE_CONFIGURATION, sapJbpResourceConfiguration);
                continue;
            }
            newProperties.put(entry.getKey(), entry.getValue());
        }
        module.setProperties(MapUtil.unmodifiable(newProperties));
    }

    protected Boolean isDataContainer(String serviceName) {
        for (Entry<String, Map<String, String>> entrySet : this.moduleNameToZdmResourcesMap.entrySet()) {
            Map<String, String> containersMap = entrySet.getValue();
            if (containersMap.get(HdiResourceTypeEnum.DATA.toString()).equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    protected String getCorrespondingAccessContainer(String dataContainerName) {
        for (Entry<String, Map<String, String>> entrySet : this.moduleNameToZdmResourcesMap.entrySet()) {
            Map<String, String> containersMap = entrySet.getValue();
            if (containersMap.get(HdiResourceTypeEnum.DATA.toString()).equals(dataContainerName)) {
                return containersMap.get(HdiResourceTypeEnum.ACCESS.toString());
            }
        }
        return null;
    }

    private List<String> getDataContainerNames() {
        List<String> dataContainerNames = new ArrayList<>();
        for (Entry<String, Map<String, String>> entrySet : this.getColorResourceNameMap().entrySet()) {
            Map<String, String> containersMap = entrySet.getValue();
            dataContainerNames.add(containersMap.get(HdiResourceTypeEnum.DATA.toString()));
        }
        return dataContainerNames;
    }

    protected int getMajorSchemaVersion() {
        return 1;
    }

    protected int getMinorSchemaVersion() {
        return 0;
    }

    public Map<String, Map<String, String>> getColorResourceNameMap() {
        return this.moduleNameToZdmResourcesMap;
    }
}
