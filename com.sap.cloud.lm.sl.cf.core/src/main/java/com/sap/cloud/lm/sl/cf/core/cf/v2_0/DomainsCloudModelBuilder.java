package com.sap.cloud.lm.sl.cf.core.cf.v2_0;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getAll;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.builders.v2_0.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;

public class DomainsCloudModelBuilder extends com.sap.cloud.lm.sl.cf.core.cf.v1_0.DomainsCloudModelBuilder {
    private ParametersChainBuilder parametersChainBuilder;

    public DomainsCloudModelBuilder(SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver,
        DeploymentDescriptor deploymentDescriptor) {
        super(systemParameters, xsPlaceholderResolver, deploymentDescriptor);
        this.parametersChainBuilder = new ParametersChainBuilder(deploymentDescriptor);
    }

    @Override
    protected List<String> getDomains(com.sap.cloud.lm.sl.mta.model.v1_0.Module module) {
        List<Map<String, Object>> parametersList = parametersChainBuilder.buildModuleChain(module.getName());
        return getAll(parametersList, SupportedParameters.DOMAIN, SupportedParameters.DOMAINS);
    }
}
