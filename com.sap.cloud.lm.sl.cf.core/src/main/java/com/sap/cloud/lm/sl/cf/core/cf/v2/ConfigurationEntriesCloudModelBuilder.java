package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MiscUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.Version;

public class ConfigurationEntriesCloudModelBuilder {

    private final String organizationName;
    private final String spaceName;
    private final String spaceGuid;

    public ConfigurationEntriesCloudModelBuilder(String organizationName, String spaceName, String spaceGuid) {
        this.organizationName = organizationName;
        this.spaceName = spaceName;
        this.spaceGuid = spaceGuid;
    }

    public Map<String, List<ConfigurationEntry>> build(DeploymentDescriptor deploymentDescriptor) {
        return deploymentDescriptor.getModules()
                                   .stream()
                                   .collect(Collectors.toMap(Module::getName,
                                                             module -> createConfigurationEntries(module, deploymentDescriptor)));
    }

    private List<ConfigurationEntry> createConfigurationEntries(Module module, DeploymentDescriptor deploymentDescriptor) {
        return getPublicProvidedDependencies(module).filter(this::hasProperties)
                                                    .map(providedDependency -> createConfigurationEntry(deploymentDescriptor,
                                                                                                        providedDependency))
                                                    .collect(Collectors.toList());
    }

    private boolean hasProperties(ProvidedDependency providedDependency) {
        return MapUtils.isNotEmpty(providedDependency.getProperties());
    }

    private Stream<ProvidedDependency> getPublicProvidedDependencies(Module module) {
        return module.getProvidedDependencies()
                     .stream()
                     .filter(ProvidedDependency::isPublic);
    }

    private ConfigurationEntry createConfigurationEntry(DeploymentDescriptor deploymentDescriptor, ProvidedDependency providedDependency) {
        String providerNid = ConfigurationEntriesUtil.PROVIDER_NID;
        String providerId = ConfigurationEntriesUtil.computeProviderId(deploymentDescriptor.getId(), providedDependency.getName());
        Version version = Version.parseVersion(deploymentDescriptor.getVersion());
        CloudTarget target = new CloudTarget(organizationName, spaceName);
        String content = JsonUtil.toJson(providedDependency.getProperties());
        List<CloudTarget> visibility = getVisibilityTargets(providedDependency);
        return new ConfigurationEntry(providerNid, providerId, version, target, content, visibility, spaceGuid);
    }

    private List<CloudTarget> getVisibilityTargets(ProvidedDependency providedDependency) {
        List<Map<String, Object>> visibleTargets = getVisibleTargets(providedDependency);
        if (visibleTargets == null) {
            return getDefaultVisibility();
        }
        Set<CloudTarget> visibility = new HashSet<>();
        visibility.add(new CloudTarget(organizationName, spaceName));

        for (Map<String, Object> visibleTarget : visibleTargets) {
            visibility.add(new CloudTarget(getElement(visibleTarget, SupportedParameters.ORGANIZATION_NAME),
                                           getElement(visibleTarget, SupportedParameters.SPACE_NAME)));
        }
        return new ArrayList<>(visibility);
    }

    protected List<Map<String, Object>> getVisibleTargets(ProvidedDependency providedDependency) {
        return MiscUtil.cast(providedDependency.getParameters()
                                               .get(SupportedParameters.VISIBILITY));
    }

    private String getElement(Map<String, Object> map, String elementName) {
        return (String) map.getOrDefault(elementName, "*");
    }

    private List<CloudTarget> getDefaultVisibility() {
        return Collections.singletonList(new CloudTarget(organizationName, "*"));
    }
}
