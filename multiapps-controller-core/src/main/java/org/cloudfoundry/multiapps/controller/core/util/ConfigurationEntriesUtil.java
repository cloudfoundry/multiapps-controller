package org.cloudfoundry.multiapps.controller.core.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.filters.ConfigurationFilter;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationEntriesUtil {

    private ConfigurationEntriesUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEntriesUtil.class);

    public static final String PROVIDER_NID = "mta";
    private static final String PROVIDER_ID_DELIMITER = ":";
    public static final String TARGET_DELIMITER = " ";
    public static final String PROVIDER_NAMESPACE_DEFAULT_VALUE = "default";

    public static String computeProviderId(String mtaId, String providedDependencyName) {
        return mtaId + PROVIDER_ID_DELIMITER + providedDependencyName;
    }

    public static boolean providerNamespaceIsEmpty(String providerNamespace, boolean considerNullAsEmpty) {
        return (considerNullAsEmpty && providerNamespace == null) || PROVIDER_NAMESPACE_DEFAULT_VALUE.equals(providerNamespace);
    }

    public static List<ConfigurationEntry> findConfigurationEntries(ConfigurationEntryService configurationEntryService,
                                                                    ConfigurationFilter filter, List<CloudTarget> cloudTargets,
                                                                    CloudTarget globalConfigTarget) {
        String providerNid = filter.getProviderNid();
        String org = null;
        String space = null;
        CloudTarget targetSpace = filter.getTargetSpace();
        if (targetSpace != null) {
            org = targetSpace.getOrganizationName();
            space = targetSpace.getSpaceName();
        }
        String providerVersion = filter.getProviderVersion();
        String providerId = filter.getProviderId();
        String providerNamespace = filter.getProviderNamespace();

        Map<String, Object> requiredContent = filter.getRequiredContent();
        LOGGER.debug("searching for configuration entries with provider nid {}, id {}, version {}, org {}, space {}, content {}, visibleTargets {}",
                     providerNid, providerId, providerVersion, org, space, requiredContent, cloudTargets);
        List<ConfigurationEntry> result = configurationEntryService.createQuery()
                                                                   .providerNid(providerNid)
                                                                   .providerId(providerId)
                                                                   .version(providerVersion)
                                                                   .providerNamespace(providerNamespace, false)
                                                                   .target(targetSpace)
                                                                   .requiredProperties(requiredContent)
                                                                   .visibilityTargets(cloudTargets)
                                                                   .list();
        if (!result.isEmpty()) {
            LOGGER.debug("result found {}", result);
            return result;
        }
        if (filter.isStrictTargetSpace() || globalConfigTarget == null) {
            return Collections.emptyList();
        }
        return findConfigurationEntriesInGlobalConfigurationSpace(configurationEntryService, providerNid, providerVersion,
                                                                  providerNamespace, providerId, requiredContent, cloudTargets,
                                                                  globalConfigTarget);
    }

    public static List<ConfigurationEntry>
           findConfigurationEntriesInGlobalConfigurationSpace(ConfigurationEntryService configurationEntryService, String providerNid,
                                                              String providerVersion, String providerNamespace, String providerId,
                                                              Map<String, Object> requiredContent, List<CloudTarget> cloudTargets,
                                                              CloudTarget globalConfigTarget) {
        LOGGER.debug("searching for configuration entries with provider nid {}, id {}, version {}, global config space space {}, content {}, visibleTargets {}",
                     providerNid, providerId, providerVersion, globalConfigTarget, requiredContent, cloudTargets);
        return configurationEntryService.createQuery()
                                        .providerNid(providerNid)
                                        .providerId(providerId)
                                        .version(providerVersion)
                                        .providerNamespace(providerNamespace, true)
                                        .target(globalConfigTarget)
                                        .requiredProperties(requiredContent)
                                        .visibilityTargets(cloudTargets)
                                        .list();
    }

    public static CloudTarget getGlobalConfigTarget(ApplicationConfiguration configuration) {
        String globalConfigSpace = configuration.getGlobalConfigSpace();
        String deployServiceOrgName = configuration.getOrgName();
        if (deployServiceOrgName == null || globalConfigSpace == null) {
            return null;
        }

        return new CloudTarget(deployServiceOrgName, globalConfigSpace);
    }

    public static String computeTargetSpace(CloudTarget target) {
        return target.getOrganizationName() + TARGET_DELIMITER + target.getSpaceName();
    }

    public static CloudTarget createImplicitCloudTarget(String targetSpace) {
        if (targetSpace == null) {
            return null;
        }
        Pattern whitespacePattern = Pattern.compile("\\S+\\s+\\S+");
        Matcher matcher = whitespacePattern.matcher(targetSpace);

        if (!matcher.find()) {
            throw new ParsingException("Target does not contain 'org' and 'space' parameters");
        }

        String[] orgAndSpace = targetSpace.split("\\s+");
        return new CloudTarget(orgAndSpace[0], orgAndSpace[1]);
    }

    public static CloudTarget splitTargetSpaceValue(String value) {
        if (StringUtils.isEmpty(value)) {
            return new CloudTarget("", "");
        }

        Pattern whitespacePattern = Pattern.compile("\\s+");
        Matcher matcher = whitespacePattern.matcher(value);
        if (!matcher.find()) {
            return new CloudTarget("", value);
        }

        String[] orgSpace = value.split("\\s+", 2);
        return new CloudTarget(orgSpace[0], orgSpace[1]);
    }

    public static ConfigurationEntry setContent(ConfigurationEntry entry, String newContent) {
        return new ConfigurationEntry(entry.getId(),
                                      entry.getProviderNid(),
                                      entry.getProviderId(),
                                      entry.getProviderVersion(),
                                      entry.getProviderNamespace(),
                                      entry.getTargetSpace(),
                                      newContent,
                                      entry.getVisibility(),
                                      entry.getSpaceId(),
                                      entry.getContentId());
    }

}
