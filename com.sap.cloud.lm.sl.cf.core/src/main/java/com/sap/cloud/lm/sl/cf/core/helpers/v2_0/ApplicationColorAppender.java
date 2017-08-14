package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.ResourceTypeEnum;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency.RequiredDependencyBuilder;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource.ResourceBuilder;

public class ApplicationColorAppender extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ApplicationColorAppender {

    public ApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        super(deployedMtaColor, applicationColor);
    }

    @Override
    public void visit(ElementContext context, com.sap.cloud.lm.sl.mta.model.v1_0.Module module) {
        visit(context, (Module) module);
    }

    private void visit(ElementContext context, Module module) {
        Map<String, Object> parameters = new TreeMap<>(module.getParameters());
        parameters.put(SupportedParameters.APP_NAME, computeAppName(module));
        module.setParameters(parameters);
    }

    @Override
    protected String computeAppName(com.sap.cloud.lm.sl.mta.model.v1_0.Module module) {
        return this.computeAppName((Module) module);
    }

    protected String computeAppName(Module module) {
        Map<String, Object> moduleParameters = module.getParameters();
        String moduleName = module.getName();
        return this.getAppName(moduleParameters, moduleName);
    }

    @Override
    protected void handleHdiModule(com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor descriptor,
        com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler descriptorHandler, com.sap.cloud.lm.sl.mta.model.v1_0.Module module) {
        handleHdiModule((DeploymentDescriptor) descriptor, (DescriptorHandler) descriptorHandler, (Module) module);
    }

    protected void handleHdiModule(DeploymentDescriptor descriptor, DescriptorHandler descriptorHandler, Module module) {
        List<RequiredDependency> nonHdiRequiredDependencies = new ArrayList<>();
        List<Resource> requiredHdiResources = new ArrayList<>();
        List<RequiredDependency> requiredDependencies = module.getRequiredDependencies2_0();
        for (RequiredDependency requiredDependency : requiredDependencies) {
            Resource resource = (Resource) descriptorHandler.findResource(descriptor, requiredDependency.getName());
            if (resource == null) {
                throw new SLException(format(RESOURCE_IS_NOT_FOUND_ERROR, requiredDependency.getName()));
            }
            if (resource.getType().equals(ResourceTypeEnum.HDI_CONTAINER.toString())) {
                requiredHdiResources.add(resource);
                continue;
            }
            nonHdiRequiredDependencies.add(requiredDependency);
        }
        addZdmResources(descriptor, module, requiredHdiResources, nonHdiRequiredDependencies);
        addZdmProperties(module);
    }

    @SuppressWarnings("unchecked")
    private void addZdmResources(DeploymentDescriptor descriptor, Module module, List<Resource> requiredHdiResources,
        List<RequiredDependency> nonHdiRequiredDependencies) {
        List<Resource> newRequiredHdiResources = (List<Resource>) createNewHdiResources(module, requiredHdiResources);
        requiredHdiResources.addAll(newRequiredHdiResources);
        List<Resource> descriptorResources = new ArrayList<>(descriptor.getResources2_0());

        for (Resource newHdiResource : newRequiredHdiResources) {
            descriptorResources.add(newHdiResource);
        }

        descriptor.setResources2_0(descriptorResources);
        RequiredDependency.RequiredDependencyBuilder requiredDependencyBuilder = getRequiredDependencyBuilder();

        List<RequiredDependency> allRequiredDependencies = new ArrayList<>();
        allRequiredDependencies.addAll(nonHdiRequiredDependencies);

        for (Resource hdiResource : requiredHdiResources) {
            requiredDependencyBuilder.setName(hdiResource.getName());
            RequiredDependency requiredDependency = requiredDependencyBuilder.build();
            allRequiredDependencies.add(requiredDependency);
        }

        module.setRequiredDependencies2_0(allRequiredDependencies);
    }

    @Override
    protected void bindModuleToNewAccessContainer(com.sap.cloud.lm.sl.mta.model.v1_0.Module module) {
        bindModuleToNewAccessContainer((Module) module);
    }

    private void bindModuleToNewAccessContainer(Module module) {
        List<RequiredDependency> requiredDependencies = module.getRequiredDependencies2_0();
        List<RequiredDependency> newRequiredDependencies = new ArrayList<>();
        RequiredDependency.RequiredDependencyBuilder requiredDependencyBuilder = getRequiredDependencyBuilder();

        for (RequiredDependency requiredDependency : requiredDependencies) {
            String requiredDependencyName = requiredDependency.getName();
            if (!isDataContainer(requiredDependencyName)) {
                newRequiredDependencies.add(requiredDependency);
                continue;
            }
            requiredDependencyBuilder.setName(getCorrespondingAccessContainer(requiredDependencyName));
            RequiredDependency newRequiredDependency = requiredDependencyBuilder.build();
            newRequiredDependencies.add(newRequiredDependency);
        }

        module.setRequiredDependencies2_0(newRequiredDependencies);
    }

    protected RequiredDependencyBuilder getRequiredDependencyBuilder() {
        return new RequiredDependency.RequiredDependencyBuilder();
    }

    @Override
    protected ResourceBuilder getResourceBuilder() {
        return new ResourceBuilder();
    }

    @Override
    protected int getMajorSchemaVersion() {
        return 2;
    }
}
