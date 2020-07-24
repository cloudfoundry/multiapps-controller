package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.core.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.Version;

public class ConfigurationEntriesCloudModelBuilder {

    private final String organizationName;
    private final String spaceName;
    private final String spaceGuid;
    private final String namespace;

    public ConfigurationEntriesCloudModelBuilder(String organizationName, String spaceName, String spaceGuid, String namespace) {
        this.organizationName = organizationName;
        this.spaceName = spaceName;
        this.spaceGuid = spaceGuid;
        this.namespace = namespace;
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
        Version providerVersion = Version.parseVersion(deploymentDescriptor.getVersion());
        String providerNamespace = namespace;
        CloudTarget target = new CloudTarget(organizationName, spaceName);
        String content = JsonUtil.toJson(providedDependency.getProperties());
        List<CloudTarget> visibility = getVisibilityTargets(providedDependency);
        return new ConfigurationEntry(providerNid,
                                      providerId,
                                      providerVersion,
                                      providerNamespace,
                                      target,
                                      content,
                                      visibility,
                                      spaceGuid,
                                      null);
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
