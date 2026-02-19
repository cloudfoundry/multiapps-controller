package org.cloudfoundry.multiapps.controller.web.configuration.service;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.web.Constants;

public class ObjectStoreServiceInfoCreator {

    public List<ObjectStoreServiceInfo> getAllProvidersServiceInfo(Map<String, Object> credentials) {
        return List.of(createServiceInfoForAws(credentials), createServiceInfoForAliCloud(credentials),
                       createServiceInfoForCcee(credentials), createServiceInfoForAzure(credentials), createServiceInfoForGcp(credentials));
    }

    private ObjectStoreServiceInfo createServiceInfoForAws(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get(Constants.ACCESS_KEY_ID);
        String secretAccessKey = (String) credentials.get(Constants.SECRET_ACCESS_KEY);
        String bucket = (String) credentials.get(Constants.BUCKET);
        String host = (String) credentials.get(Constants.HOST);
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.AWS_S_3)
                                              .identity(accessKeyId)
                                              .credential(secretAccessKey)
                                              .container(bucket)
                                              .host(host)
                                              .build();
    }

    private ObjectStoreServiceInfo createServiceInfoForAliCloud(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get(Constants.ACCESS_KEY_ID);
        String secretAccessKey = (String) credentials.get(Constants.SECRET_ACCESS_KEY);
        String bucket = (String) credentials.get(Constants.BUCKET);
        String region = (String) credentials.get(Constants.REGION);
        String endpoint = (String) credentials.get(Constants.ENDPOINT);
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.ALIYUN_OSS)
                                              .identity(accessKeyId)
                                              .credential(secretAccessKey)
                                              .container(bucket)
                                              .endpoint(endpoint)
                                              .region(region)
                                              .build();
    }

    private ObjectStoreServiceInfo createServiceInfoForCcee(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get(Constants.ACCESS_KEY_ID);
        String containerName = (String) credentials.get(Constants.CONTAINER_NAME_WITH_DASH);
        String endpointUrl = (String) credentials.get(Constants.ENDPOINT_URL);
        String region = (String) credentials.get(Constants.REGION);
        String secretAccessKey = (String) credentials.get(Constants.SECRET_ACCESS_KEY);
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.AWS_S_3)
                                              .identity(accessKeyId)
                                              .container(containerName)
                                              .endpoint(endpointUrl)
                                              .region(region)
                                              .credential(secretAccessKey)
                                              .build();
    }

    private ObjectStoreServiceInfo createServiceInfoForAzure(Map<String, Object> credentials) {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.AZUREBLOB)
                                              .credentials(credentials)
                                              .build();
    }

    private ObjectStoreServiceInfo createServiceInfoForGcp(Map<String, Object> credentials) {
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider(Constants.GOOGLE_CLOUD_STORAGE)
                                              .credentials(credentials)
                                              .build();
    }
}
