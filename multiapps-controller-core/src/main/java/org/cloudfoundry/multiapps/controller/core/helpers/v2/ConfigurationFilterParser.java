package org.cloudfoundry.multiapps.controller.core.helpers.v2;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.filters.ConfigurationFilter;
import org.cloudfoundry.multiapps.mta.builders.v2.PropertiesChainBuilder;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

public class ConfigurationFilterParser {

    private static final String NEW_SYNTAX_FILTER = "configuration";
    private static final String OLD_SYNTAX_FILTER = "mta-provides-dependency";

    protected final CloudTarget currentTarget;
    protected final PropertiesChainBuilder chainBuilder;
    protected final String targetedNamespace;

    public ConfigurationFilterParser(CloudTarget currentTarget, PropertiesChainBuilder chainBuilder, String namespace) {
        this.currentTarget = currentTarget;
        this.chainBuilder = chainBuilder;
        this.targetedNamespace = namespace;
    }

    public ConfigurationFilter parse(Resource resource) {
        String type = getType(resource);
        if (OLD_SYNTAX_FILTER.equals(type)) {
            return parseOldSyntaxFilter(resource);
        }
        if (NEW_SYNTAX_FILTER.equals(type)) {
            return parseNewSyntaxFilter(resource);
        }
        return null;
    }

    private String getType(Resource resource) {
        Map<String, Object> mergedParameters = PropertiesUtil.mergeProperties(chainBuilder.buildResourceChain(resource.getName()));
        return (String) mergedParameters.get(SupportedParameters.TYPE);
    }

    @Deprecated
    private ConfigurationFilter parseOldSyntaxFilter(Resource resource) {
        Map<String, Object> parameters = getParameters(resource);
        String mtaId = PropertiesUtil.getRequiredParameter(parameters, SupportedParameters.DEPRECATED_CONFIG_MTA_ID);
        String mtaProvidesDependency = PropertiesUtil.getRequiredParameter(parameters,
                                                                           SupportedParameters.DEPRECATED_CONFIG_MTA_PROVIDES_DEPENDENCY);
        String mtaVersion = PropertiesUtil.getRequiredParameter(parameters, SupportedParameters.DEPRECATED_CONFIG_MTA_VERSION);
        String providerId = ConfigurationEntriesUtil.computeProviderId(mtaId, mtaProvidesDependency);
        return new ConfigurationFilter(ConfigurationEntriesUtil.PROVIDER_NID, providerId, mtaVersion, null, currentTarget, null);
    }

    private ConfigurationFilter parseNewSyntaxFilter(Resource resource) {
        Map<String, Object> parameters = getParameters(resource);
        String version = PropertiesUtil.getOptionalParameter(parameters, SupportedParameters.VERSION);
        String nid = PropertiesUtil.getOptionalParameter(parameters, SupportedParameters.PROVIDER_NID);
        String namespace = getEffectiveNamespace(parameters);
        String pid = PropertiesUtil.getOptionalParameter(parameters, SupportedParameters.PROVIDER_ID);
        Map<String, Object> filter = PropertiesUtil.getOptionalParameter(parameters, SupportedParameters.FILTER);
        Map<String, Object> target = PropertiesUtil.getOptionalParameter(parameters, SupportedParameters.TARGET);
        boolean hasExplicitTarget = target != null;
        CloudTarget cloudTarget = hasExplicitTarget ? parseSpaceTarget(target) : currentTarget;
        return new ConfigurationFilter(nid, pid, version, namespace, cloudTarget, filter, hasExplicitTarget);
    }

    private CloudTarget parseSpaceTarget(Map<String, Object> target) {
        String organizationName = PropertiesUtil.getRequiredParameter(target, SupportedParameters.ORGANIZATION_NAME);
        String spaceName = PropertiesUtil.getRequiredParameter(target, SupportedParameters.SPACE_NAME);
        return new CloudTarget(organizationName, spaceName);
    }

    public Map<String, Object> getParameters(Resource resource) {
        return resource.getParameters();
    }

    public String getEffectiveNamespace(Map<String, Object> filterParameters) {
        String filterNamespace = PropertiesUtil.getOptionalParameter(filterParameters, SupportedParameters.PROVIDER_NAMESPACE);
        if (filterNamespace != null) {
            return filterNamespace;
        }

        return this.targetedNamespace;
    }
}
