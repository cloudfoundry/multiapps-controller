package com.sap.cloud.lm.sl.cf.core.util;

import java.util.List;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.common.util.Pair;

public class ConfigurationEntriesUtil {

    public static final String PROVIDER_NID = "mta";
    private static final String PROVIDER_ID_DELIMITER = ":";
    private static final String TARGET_DELIMITER = " ";

    public static String computeProviderId(String mtaId, String providedDependencyName) {
        return mtaId + PROVIDER_ID_DELIMITER + providedDependencyName;
    }

    public static Pair<String, String> getMtaIdAndProvidedDependencyName(String providerId) {
        String[] providerIdSplitted = providerId.split(PROVIDER_ID_DELIMITER, 2);
        return new Pair<String, String>(providerIdSplitted[0], providerIdSplitted[1]);
    }

    public static List<ConfigurationEntry> findConfigurationEntries(ConfigurationEntryDao dao, ConfigurationFilter filter) {
        String providerNid = filter.getProviderNid();
        String targetSpace = filter.getTargetSpace();
        String providerVersion = filter.getProviderVersion();
        String providerId = filter.getProviderId();
        return dao.find(providerNid, providerId, providerVersion, targetSpace, filter.getRequiredContent(), null);
    }

    public static String computeTargetSpace(Pair<String, String> target) {
        return target._1 + TARGET_DELIMITER + target._2;
    }

}
