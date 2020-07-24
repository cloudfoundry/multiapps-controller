package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Map;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.NamedParametersContainer;

public class GlobalParameters implements NamedParametersContainer {

    private static final String GLOBAL_PARAMETERS_NAME = "Global";

    private final DeploymentDescriptor deploymentDescriptor;

    public GlobalParameters(DeploymentDescriptor deploymentDescriptor) {
        this.deploymentDescriptor = deploymentDescriptor;
    }

    @Override
    public String getName() {
        return GLOBAL_PARAMETERS_NAME;
    }

    @Override
    public Map<String, Object> getParameters() {
        return deploymentDescriptor.getParameters();
    }

    @Override
    public Object setParameters(Map<String, Object> parameters) {
        return deploymentDescriptor.setParameters(parameters);
    }
}
