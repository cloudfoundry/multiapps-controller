package com.sap.cloud.lm.sl.cf.core.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
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
        List<CloudTarget> cloudTargets) {
        String providerNid = filter.getProviderNid();
        String org = null;
        String space = null;
        CloudTarget targetSpace = filter.getTargetSpace();
        if(targetSpace != null){
            org=targetSpace.getOrg();
            space = targetSpace.getSpace();
        }
        String providerVersion = filter.getProviderVersion();
        String providerId = filter.getProviderId();

        Map<String, Object> requiredContent = filter.getRequiredContent();
        LOGGER.debug("searching for configuration entries with provider nid {}, id {}, version {}, org {}, space {}, content {}, visibleTargets {}",
            providerNid, providerId, providerVersion, org, space, requiredContent, cloudTargets);
        List<ConfigurationEntry> result = dao.find(providerNid, providerId, providerVersion, targetSpace, requiredContent, null,
            cloudTargets);
        if (!result.isEmpty()) {
            LOGGER.debug("result found {}", result);
            return result;
        }
        if (filter.isStrictTargetSpace()) {
            return Collections.emptyList();
        }
        return findConfigurationEntriesInGlobalConfigurationSpace(dao, providerNid, providerVersion, providerId, requiredContent,
            cloudTargets);
    }

    public static List<ConfigurationEntry> findConfigurationEntriesInGlobalConfigurationSpace(ConfigurationEntryDao dao, String providerNid,
        String providerVersion, String providerId, Map<String, Object> requiredContent, List<CloudTarget> cloudTargets) {
        String globalConfigSpace = ConfigurationUtil.getGlobalConfigSpace();
        String deployServiceOrgName = ConfigurationUtil.getOrgName();
        if (deployServiceOrgName == null || globalConfigSpace == null) {
            return Collections.emptyList();
        }

        CloudTarget target = new CloudTarget(deployServiceOrgName, globalConfigSpace);
        LOGGER.debug(
            "searching for configuration entries with provider nid {}, id {}, version {}, global config space space {}, content {}, visibleTargets {}",
            providerNid, providerId, providerVersion, target, requiredContent, cloudTargets);
        return dao.find(providerNid, providerId, providerVersion, target, requiredContent, null, cloudTargets);
    }

    public static String computeTargetSpace(Pair<String, String> target) {
        return target._1 + TARGET_DELIMITER + target._2;
    }

}
