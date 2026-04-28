package org.cloudfoundry.multiapps.controller.web.configuration.service;

import org.cloudfoundry.multiapps.controller.web.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectStoreServiceInfoCreator {

    public List<ObjectStoreServiceInfo> getAllProvidersServiceInfo(Map<String, Object> credentials) {
        return List.of(
            createServiceInfoForAws(credentials),
            createServiceInfoForCcee(credentials),
            createServiceInfoForAliCloud(credentials),
            createServiceInfoForAzure(credentials),
            createServiceInfoForGcp(credentials)
        );
    }

    private ObjectStoreServiceInfo createServiceInfoForAws(Map<String, Object> credentials) {
        return createObjectStoreServiceInfo(Constants.AWS_S_3, credentials);
    }

    private ObjectStoreServiceInfo createServiceInfoForCcee(Map<String, Object> credentials) {
        Map<String, Object> translated = new HashMap<>(credentials);
        Object containerName = credentials.get(Constants.CONTAINER_NAME_WITH_DASH);
        if (containerName != null) {
            translated.put(Constants.BUCKET, containerName);
        }
        Object endpointUrl = credentials.get(Constants.ENDPOINT_URL);
        if (endpointUrl != null) {
            translated.put(Constants.ENDPOINT, endpointUrl);
        }
        return createObjectStoreServiceInfo(Constants.AWS_S_3, translated);
    }

    private ObjectStoreServiceInfo createServiceInfoForAliCloud(Map<String, Object> credentials) {
        return createObjectStoreServiceInfo(Constants.ALIYUN_OSS, credentials);
    }

    private ObjectStoreServiceInfo createServiceInfoForAzure(Map<String, Object> credentials) {
        return createObjectStoreServiceInfo(Constants.AZUREBLOB, credentials);
    }

    private ObjectStoreServiceInfo createServiceInfoForGcp(Map<String, Object> credentials) {
        return createObjectStoreServiceInfo(Constants.GOOGLE_CLOUD_STORAGE, credentials);
    }

    private ObjectStoreServiceInfo createObjectStoreServiceInfo(String provider, Map<String, Object> credentials) {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(provider)
                                              .credentials(credentials)
                                              .build();
    }
}