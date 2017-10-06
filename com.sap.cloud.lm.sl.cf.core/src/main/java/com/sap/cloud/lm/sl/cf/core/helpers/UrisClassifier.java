package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;

public class UrisClassifier {

    private XsPlaceholderResolver xsPlaceholderResolver;

    public UrisClassifier(XsPlaceholderResolver xsPlaceholderResolver) {
        this.xsPlaceholderResolver = xsPlaceholderResolver;
    }

    public List<String> getCustomUris(DeployedMtaModule deployedModule) {
        if (deployedModule == null) {
            return Collections.emptyList();
        }
        List<String> allUris = getUrisWithoutScheme(deployedModule);
        List<String> descriptorDefinedUris = getDescriptorDefinedUris(deployedModule);
        List<String> customUris = new ArrayList<>(allUris);
        customUris.removeAll(descriptorDefinedUris);
        return customUris;
    }

    public List<String> getDescriptorDefinedUris(DeployedMtaModule deployedModule) {
        if (deployedModule == null) {
            return Collections.emptyList();
        }
        List<String> allUris = getUrisWithoutScheme(deployedModule);
        List<String> descriptorDefinedUris = getDescriptorDefinedUrisOrDefault(deployedModule, allUris);
        descriptorDefinedUris.retainAll(allUris);
        return descriptorDefinedUris;
    }

    @SuppressWarnings("unchecked")
    private List<String> getDescriptorDefinedUrisOrDefault(DeployedMtaModule deployedModule, List<String> defaultDescriptorDefinedUris) {
        Map<String, Object> attributes = deployedModule.getDeployAttributes();
        if (attributes == null || !attributes.containsKey(Constants.ATTR_DESCRIPTOR_DEFINED_URIS)) {
            return defaultDescriptorDefinedUris;
        }
        List<String> descriptorDefinedUris = (List<String>) attributes.get(Constants.ATTR_DESCRIPTOR_DEFINED_URIS);
        return xsPlaceholderResolver.resolve(descriptorDefinedUris);
    }

    private List<String> getUrisWithoutScheme(DeployedMtaModule deployedModule) {
        return UriUtil.getUrisWithoutScheme(deployedModule.getUris());
    }

}
