package com.sap.cloud.lm.sl.cf.core.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.Pair;

public class ConfigurationEntriesUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEntriesUtil.class);

    public static final String PROVIDER_NID = "mta";
    private static final String PROVIDER_ID_DELIMITER = ":";
    public static final String TARGET_DELIMITER = " ";

    public static String computeProviderId(String mtaId, String providedDependencyName) {
        return mtaId + PROVIDER_ID_DELIMITER + providedDependencyName;
    }

    public static Pair<String, String> getMtaIdAndProvidedDependencyName(String providerId) {
        String[] providerIdSplitted = providerId.split(PROVIDER_ID_DELIMITER, 2);
        return new Pair<String, String>(providerIdSplitted[0], providerIdSplitted[1]);
    }

    public static List<ConfigurationEntry> findConfigurationEntries(ConfigurationEntryDao dao, ConfigurationFilter filter,
        List<CloudTarget> cloudTargets, CloudTarget globalConfigTarget) {
        String providerNid = filter.getProviderNid();
        String org = null;
        String space = null;
        CloudTarget targetSpace = filter.getTargetSpace();
        if (targetSpace != null) {
            org = targetSpace.getOrg();
            space = targetSpace.getSpace();
        }
        String providerVersion = filter.getProviderVersion();
        String providerId = filter.getProviderId();

        Map<String, Object> requiredContent = filter.getRequiredContent();
        LOGGER.debug(
            "searching for configuration entries with provider nid {}, id {}, version {}, org {}, space {}, content {}, visibleTargets {}",
            providerNid, providerId, providerVersion, org, space, requiredContent, cloudTargets);
        List<ConfigurationEntry> result = dao.find(providerNid, providerId, providerVersion, targetSpace, requiredContent, null,
            cloudTargets);
        if (!result.isEmpty()) {
            LOGGER.debug("result found {}", result);
            return result;
        }
        if (filter.isStrictTargetSpace() || globalConfigTarget == null) {
            return Collections.emptyList();
        }
        return findConfigurationEntriesInGlobalConfigurationSpace(dao, providerNid, providerVersion, providerId, requiredContent,
            cloudTargets, globalConfigTarget);
    }

    public static List<ConfigurationEntry> findConfigurationEntriesInGlobalConfigurationSpace(ConfigurationEntryDao dao, String providerNid,
        String providerVersion, String providerId, Map<String, Object> requiredContent, List<CloudTarget> cloudTargets, CloudTarget globalConfigTarget) {
        LOGGER.debug(
            "searching for configuration entries with provider nid {}, id {}, version {}, global config space space {}, content {}, visibleTargets {}",
            providerNid, providerId, providerVersion, globalConfigTarget, requiredContent, cloudTargets);
        return dao.find(providerNid, providerId, providerVersion, globalConfigTarget, requiredContent, null, cloudTargets);
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
        return target.getOrg() + TARGET_DELIMITER + target.getSpace();
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
        CloudTarget cloudTarget = new CloudTarget(orgAndSpace[0], orgAndSpace[1]);

        return cloudTarget;
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

}
