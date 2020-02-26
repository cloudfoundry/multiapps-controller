package com.sap.cloud.lm.sl.cf.core.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cp.security.credstore.client.CredentialStorage;
import com.sap.cp.security.credstore.client.CredentialStoreClientException;
import com.sap.cp.security.credstore.client.CredentialStoreFactory;
import com.sap.cp.security.credstore.client.CredentialStoreInstance;
import com.sap.cp.security.credstore.client.CredentialStoreNamespaceInstance;
import com.sap.cp.security.credstore.client.EnvCoordinates;
import com.sap.cp.security.credstore.client.PasswordCredential;

public class ConfigurationEntriesUtil {

    private ConfigurationEntriesUtil() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEntriesUtil.class);

    public static final String PROVIDER_NID = "mta";
    private static final String PROVIDER_ID_DELIMITER = ":";
    public static final String TARGET_DELIMITER = " ";

    public static String computeProviderId(String mtaId, String providedDependencyName) {
        return mtaId + PROVIDER_ID_DELIMITER + providedDependencyName;
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

        Map<String, Object> requiredContent = filter.getRequiredContent();
        LOGGER.debug("searching for configuration entries with provider nid {}, id {}, version {}, org {}, space {}, content {}, visibleTargets {}",
                     providerNid, providerId, providerVersion, org, space, requiredContent, cloudTargets);
        List<ConfigurationEntry> result = configurationEntryService.createQuery()
                                                                   .providerNid(providerNid)
                                                                   .providerId(providerId)
                                                                   .version(providerVersion)
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
        return findConfigurationEntriesInGlobalConfigurationSpace(configurationEntryService, providerNid, providerVersion, providerId,
                                                                  requiredContent, cloudTargets, globalConfigTarget);
    }

    public static List<ConfigurationEntry>
           findConfigurationEntriesInGlobalConfigurationSpace(ConfigurationEntryService configurationEntryService, String providerNid,
                                                              String providerVersion, String providerId,
                                                              Map<String, Object> requiredContent, List<CloudTarget> cloudTargets,
                                                              CloudTarget globalConfigTarget) {
        LOGGER.debug("searching for configuration entries with provider nid {}, id {}, version {}, global config space space {}, content {}, visibleTargets {}",
                     providerNid, providerId, providerVersion, globalConfigTarget, requiredContent, cloudTargets);
        return configurationEntryService.createQuery()
                                        .providerNid(providerNid)
                                        .providerId(providerId)
                                        .version(providerVersion)
                                        .target(globalConfigTarget)
                                        .requiredProperties(requiredContent)
                                        .visibilityTargets(cloudTargets)
                                        .list();
    }
    
    private static String getCredStoreId(ConfigurationEntry entry) {
        return UUID.nameUUIDFromBytes(entry.getProviderId().getBytes()).toString();
//        return String.valueOf(entry.getProviderId().hashCode());
    }
    
    public static String getContent(ConfigurationEntry configurationEntry) {
        CredentialStoreNamespaceInstance credentialStoreNamespaceInstance = getCredentialStoreNamespaceInstance(configurationEntry);
        CredentialStorage<PasswordCredential> passwordStorage = credentialStoreNamespaceInstance.getPasswordCredentialStorage();
        PasswordCredential password1;
        String content = null;
        try {
            password1 = passwordStorage.read(getCredStoreId(configurationEntry));
            content = String.valueOf(password1.getValue());
        } catch (CredentialStoreClientException e) {
            throw new SLException(e);
        }
        LOGGER.warn("Credstore return: " + getCredStoreId(configurationEntry) + ", value: " + content);
        return content;
    }

    static CredentialStoreNamespaceInstance getCredentialStoreNamespaceInstance(ConfigurationEntry configurationEntry) {
        CredentialStoreInstance credentialStore = CredentialStoreFactory.getInstance(EnvCoordinates.DEFAULT_ENVIRONMENT);
        return credentialStore.getNamespaceInstance(configurationEntry.getSpaceId());
    }
    
    static CredentialStoreNamespaceInstance getCredentialStoreNamespaceInstance(String spaceId) {
        CredentialStoreInstance credentialStore = CredentialStoreFactory.getInstance(EnvCoordinates.DEFAULT_ENVIRONMENT);
        return credentialStore.getNamespaceInstance(spaceId);
    }
    
    public static void deleteAllCredentials(String spaceId) {
        CredentialStoreNamespaceInstance credentialStoreNamespaceInstance = getCredentialStoreNamespaceInstance(spaceId);
        try {
            credentialStoreNamespaceInstance.deleteAllCredentials();
        } catch (CredentialStoreClientException e) {
            throw new SLException(e);
        }
        
    }
    
    public static void deletePasswordCredential(ConfigurationEntry entry) {
        CredentialStoreNamespaceInstance credentialStoreNamespaceInstance = getCredentialStoreNamespaceInstance(entry);
        CredentialStorage<PasswordCredential> passwordStorage = credentialStoreNamespaceInstance.getPasswordCredentialStorage();
        
        try {
            passwordStorage.delete(getCredStoreId(entry));
        } catch (CredentialStoreClientException e) {
            LOGGER.warn("fail to delete " + getCredStoreId(entry), e);
            throw new SLException(e);
        }
    }
    
    public static void addPasswordCredential(ConfigurationEntry entry) {
        CredentialStoreNamespaceInstance credentialStoreNamespaceInstance = getCredentialStoreNamespaceInstance(entry);
        CredentialStorage<PasswordCredential> passwordStorage = credentialStoreNamespaceInstance.getPasswordCredentialStorage();
        PasswordCredential password1 = PasswordCredential.builder(getCredStoreId(entry), entry.getContent().toCharArray())
                                                         .build();
        try {
            passwordStorage.create(password1);
        } catch (CredentialStoreClientException e) {
            LOGGER.warn("fail to delete password credential " + getCredStoreId(entry), e);
            throw new SLException(e);
        }
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

}
