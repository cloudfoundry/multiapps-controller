package com.sap.cloud.lm.sl.cf.core.cf.v1_0;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.common.util.CommonUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.ParametersContainer;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.ProvidedDependency;

public class ConfigurationEntriesCloudModelBuilder {

    private String orgName;
    private String spaceName;
    private String spaceId;

    public ConfigurationEntriesCloudModelBuilder(String orgName, String spaceName, String spaceId) {
        this.orgName = orgName;
        this.spaceName = spaceName;
        this.spaceId = spaceId;
    }

    public Map<String, List<ConfigurationEntry>> build(DeploymentDescriptor deploymentDescriptor) {
        Map<String, List<ProvidedDependency>> publicProvidedDependencies = getPublicProvidedDependencies(deploymentDescriptor);
        return createConfigurationEntries(deploymentDescriptor, publicProvidedDependencies);
    }

    private Map<String, List<ProvidedDependency>> getPublicProvidedDependencies(DeploymentDescriptor deploymentDescriptor) {
        Map<String, List<ProvidedDependency>> resultMap = new HashMap<>();
        for (Module module : deploymentDescriptor.getModules1_0()) {
            resultMap.put(module.getName(), getPublicProvidedDependencies(module));
        }
        return resultMap;
    }

    private List<ProvidedDependency> getPublicProvidedDependencies(Module module) {
        return module.getProvidedDependencies1_0()
            .stream()
            .filter(CloudModelBuilderUtil::isPublic)
            .collect(Collectors.toList());
    }

    private Map<String, List<ConfigurationEntry>> createConfigurationEntries(DeploymentDescriptor deploymentDescriptor,
        Map<String, List<ProvidedDependency>> providedDependencies) {
        Map<String, List<ConfigurationEntry>> result = new TreeMap<>();
        for (Entry<String, List<ProvidedDependency>> providedDependency : providedDependencies.entrySet()) {
            result.put(providedDependency.getKey(),
                createConfigurationEntriesForModule(deploymentDescriptor, providedDependency.getValue()));
        }
        return result;
    }

    private List<ConfigurationEntry> createConfigurationEntriesForModule(DeploymentDescriptor deploymentDescriptor,
        List<ProvidedDependency> providedDependencies) {
        return providedDependencies.stream()
            .map(providedDependency -> createConfigurationEntry(deploymentDescriptor, providedDependency))
            .collect(Collectors.toList());
    }

    private ConfigurationEntry createConfigurationEntry(DeploymentDescriptor deploymentDescriptor, ProvidedDependency providedDependency) {
        String providerNid = ConfigurationEntriesUtil.PROVIDER_NID;
        String providerId = ConfigurationEntriesUtil.computeProviderId(deploymentDescriptor.getId(), providedDependency.getName());
        Version version = Version.parseVersion(deploymentDescriptor.getVersion());
        CloudTarget target = new CloudTarget(orgName, spaceName);
        String content = JsonUtil.toJson(providedDependency.getProperties());
        List<CloudTarget> visibility = getVisibilityTargets(providedDependency);
        return new ConfigurationEntry(providerNid, providerId, version, target, content, visibility, spaceId);
    }

    private List<CloudTarget> getVisibilityTargets(ProvidedDependency providedDependency) {
        List<Map<String, Object>> visibleTargets = getVisibleTargets(providedDependency);
        if (visibleTargets == null) {
            return getDefaultVisibility();
        }
        Set<CloudTarget> visibility = new HashSet<>();
        visibility.add(new CloudTarget(orgName, spaceName));

        for (Map<String, Object> visibleTarget : visibleTargets) {
            visibility.add(
                new CloudTarget(getElement(visibleTarget, SupportedParameters.ORG), getElement(visibleTarget, SupportedParameters.SPACE)));
        }
        return new ArrayList<>(visibility);
    }

    protected List<Map<String, Object>> getVisibleTargets(ProvidedDependency providedDependency) {
        if (!(providedDependency instanceof ParametersContainer)) {
            return null;
        }
        ParametersContainer dependency = (ParametersContainer) providedDependency;
        return CommonUtil.cast(dependency.getParameters()
            .get(SupportedParameters.VISIBILITY));
    }

    private String getElement(Map<String, Object> map, String elementName) {
        if (!map.containsKey(elementName)) {
            return "*";
        }
        return (String) map.get(elementName);
    }

    private List<CloudTarget> getDefaultVisibility() {
        List<CloudTarget> visibility = new ArrayList<>();
        visibility.add(new CloudTarget(orgName, "*"));
        return visibility;
    }
}
