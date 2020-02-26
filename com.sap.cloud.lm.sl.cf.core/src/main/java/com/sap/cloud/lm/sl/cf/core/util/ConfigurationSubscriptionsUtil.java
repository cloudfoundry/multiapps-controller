package com.sap.cloud.lm.sl.cf.core.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cp.security.credstore.client.CredentialStorage;
import com.sap.cp.security.credstore.client.CredentialStoreClientException;
import com.sap.cp.security.credstore.client.CredentialStoreFactory;
import com.sap.cp.security.credstore.client.CredentialStoreInstance;
import com.sap.cp.security.credstore.client.CredentialStoreNamespaceInstance;
import com.sap.cp.security.credstore.client.EnvCoordinates;
import com.sap.cp.security.credstore.client.KeyCredential;

public class ConfigurationSubscriptionsUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationSubscriptionsUtil.class);
    
    public static void deleteKeyCredential(ConfigurationSubscription entry) {
        CredentialStoreNamespaceInstance credentialStoreNamespaceInstance = getCredentialStoreNamespaceInstance(entry);
        CredentialStorage<KeyCredential> keyStorage = credentialStoreNamespaceInstance.getKeyCredentialStorage();
        try {
            LOGGER.warn("Deleting key from credstore with name: " + getCredStoreId(entry));
            keyStorage.delete(getCredStoreId(entry));
        } catch (CredentialStoreClientException e) {
            LOGGER.warn("fail to delete password credential " + getCredStoreId(entry), e);
            throw new SLException(e);
        }
    }
    
    public static void addKeyCredential(ConfigurationSubscription entry) {
        CredentialStoreNamespaceInstance credentialStoreNamespaceInstance = getCredentialStoreNamespaceInstance(entry);
        CredentialStorage<KeyCredential> keyStorage = credentialStoreNamespaceInstance.getKeyCredentialStorage();
        KeyCredential key1 = KeyCredential.builder(getCredStoreId(entry), JsonUtil.toJsonBinary(entry.getModuleDto())).build();
        try {
            LOGGER.warn("Adding key in credstore with name: " + getCredStoreId(entry)+ " and value: " + JsonUtil.toJson(entry.getModuleDto()));
            keyStorage.write(key1);
        } catch (CredentialStoreClientException e) {
            LOGGER.warn("fail to delete password credential " + getCredStoreId(entry), e);
            throw new SLException(e);
        }
    }
    
    public static ConfigurationSubscription.ModuleDto getModuleDto(ConfigurationSubscription entry) {
        CredentialStoreNamespaceInstance credentialStoreNamespaceInstance = getCredentialStoreNamespaceInstance(entry);
        CredentialStorage<KeyCredential> keyStorage = credentialStoreNamespaceInstance.getKeyCredentialStorage();
        ConfigurationSubscription.ModuleDto module = null;
        try {
            KeyCredential key1 = keyStorage.read(getCredStoreId(entry));
            module = JsonUtil.fromJson(new String(key1.getValue(), StandardCharsets.UTF_8), ConfigurationSubscription.ModuleDto.class);
        } catch (CredentialStoreClientException e) {
            throw new SLException(e);
        }
        LOGGER.warn("Credstore return key with name: " + getCredStoreId(entry)+ " and value: " + JsonUtil.toJson(module));
        return module;
    }
    
    private static CredentialStoreNamespaceInstance getCredentialStoreNamespaceInstance(ConfigurationSubscription entry) {
        CredentialStoreInstance credentialStore = CredentialStoreFactory.getInstance(EnvCoordinates.DEFAULT_ENVIRONMENT);
        return credentialStore.getNamespaceInstance(entry.getSpaceId());
    }
    
    private static String getCredStoreId(ConfigurationSubscription entry) {
        String entryUniqeKey = entry.getMtaId()+ ":" + entry.getAppName() + ":" + entry.getResourceDto().getName();
        return UUID.nameUUIDFromBytes(entryUniqeKey.getBytes()).toString();
    }
    
}
