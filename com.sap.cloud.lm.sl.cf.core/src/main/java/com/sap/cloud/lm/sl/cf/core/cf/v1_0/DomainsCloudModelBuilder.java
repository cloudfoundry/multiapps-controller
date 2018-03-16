package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getAll;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.builders.v1_0.PropertiesChainBuilder;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;

public class DomainsCloudModelBuilder {
    private SystemParameters systemParameters;
    private XsPlaceholderResolver xsPlaceholderResolver;
    private PropertiesChainBuilder propertiesChainBuilder;
    private DeploymentDescriptor deploymentDescriptor;

    public DomainsCloudModelBuilder(SystemParameters systemParameters, XsPlaceholderResolver xsPlaceholderResolver,
        DeploymentDescriptor deploymentDescriptor) {
        this.systemParameters = systemParameters;
        this.xsPlaceholderResolver = xsPlaceholderResolver;
        this.propertiesChainBuilder = new PropertiesChainBuilder(deploymentDescriptor);
        this.deploymentDescriptor = deploymentDescriptor;
    }

    public List<String> build() {
        Set<String> domains = new TreeSet<>();
        String defaultDomain = (String) systemParameters.getGeneralParameters()
            .getOrDefault(SupportedParameters.DEFAULT_DOMAIN, null);
        for (Module module : deploymentDescriptor.getModules1_0()) {
            domains.addAll(getDomains(module));
        }
        if (xsPlaceholderResolver.getDefaultDomain() != null) {
            domains.remove(xsPlaceholderResolver.getDefaultDomain());
        }
        if (defaultDomain != null) {
            domains.remove(defaultDomain);
        }
        return new ArrayList<>(domains);
    }

    protected List<String> getDomains(Module module) {
        List<Map<String, Object>> propertiesList = propertiesChainBuilder.buildModuleChain(module.getName());
        return getAll(propertiesList, SupportedParameters.DOMAIN, SupportedParameters.DOMAINS);
    }
}
