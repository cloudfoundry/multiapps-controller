package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.PROVIDER_NID;
import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.computeTargetSpace;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getOptionalParameter;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getRequiredParameter;
import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.mergeProperties;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.builders.v1_0.PropertiesChainBuilder;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public class ConfigurationFilterParser {

    private static final String NEW_SYNTAX_FILTER = "configuration";
    private static final String OLD_SYNTAX_FILTER = "mta-provides-dependency";

    protected TargetPlatformType platformType;
    protected TargetPlatform platform;
    protected PropertiesChainBuilder chainBuilder;

    public ConfigurationFilterParser(TargetPlatformType platformType, TargetPlatform platform, PropertiesChainBuilder chainBuilder) {
        this.platformType = platformType;
        this.platform = platform;
        this.chainBuilder = chainBuilder;
    }

    public ConfigurationFilter parse(Resource resource) throws ContentException {
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
        Map<String, Object> mergedParameters = mergeProperties(chainBuilder.buildResourceChain(resource.getName()));
        return (String) mergedParameters.get(SupportedParameters.TYPE);
    }

    private ConfigurationFilter parseOldSyntaxFilter(Resource resource) throws ContentException {
        Map<String, Object> parameters = getParameters(resource);
        String mtaId = getRequiredParameter(parameters, SupportedParameters.MTA_ID);
        String target = computeTargetSpace(getCurrentOrgAndSpace());
        String mtaProvidesDependency = getRequiredParameter(parameters, SupportedParameters.MTA_PROVIDES_DEPENDENCY);
        String mtaVersion = getRequiredParameter(parameters, SupportedParameters.MTA_VERSION);
        String providerId = ConfigurationEntriesUtil.computeProviderId(mtaId, mtaProvidesDependency);
        return new ConfigurationFilter(PROVIDER_NID, providerId, mtaVersion, target, null);
    }

    private ConfigurationFilter parseNewSyntaxFilter(Resource resource) throws ContentException {
        Map<String, Object> parameters = getParameters(resource);
        String version = getOptionalParameter(parameters, SupportedParameters.VERSION);
        String namespaceId = getOptionalParameter(parameters, SupportedParameters.PROVIDER_NID);
        String pid = getOptionalParameter(parameters, SupportedParameters.PROVIDER_ID);
        Map<String, String> filter = getOptionalParameter(parameters, SupportedParameters.FILTER);
        return new ConfigurationFilter(namespaceId, pid, version, getTargetSpace(parameters), filter);
    }

    private String getTargetSpace(Map<String, Object> parameters) throws ContentException {
        Map<String, Object> target = getOptionalParameter(parameters, SupportedParameters.TARGET);
        if (target != null) {
            return computeTargetSpace(
                new Pair<>(getRequiredParameter(target, SupportedParameters.ORG), getRequiredParameter(target, SupportedParameters.SPACE)));
        }
        return null;
    }

    protected Map<String, Object> getParameters(Resource resource) {
        return resource.getProperties();
    }

    protected Pair<String, String> getCurrentOrgAndSpace() {
        return new OrgAndSpaceHelper(platform, platformType).getOrgAndSpace();
    }

}
