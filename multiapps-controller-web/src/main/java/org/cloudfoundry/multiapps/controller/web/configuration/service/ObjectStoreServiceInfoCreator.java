package org.cloudfoundry.multiapps.controller.web.configuration.service;

import java.util.Map;

import io.pivotal.cfenv.core.CfService;

public class ObjectStoreServiceInfoCreator {

    private static final String OBJECT_STORE_AWS_PLAN = "s3-standard";
    private static final String OBJECT_STORE_AZURE_PLAN = "azure-standard";
    private static final String OBJECT_STORE_ALICLOUD_PLAN = "oss-standard";

    public ObjectStoreServiceInfo createServiceInfo(CfService service) {
        String plan = service.getPlan();
        Map<String, Object> credentials = service.getCredentials()
                                                 .getMap();
        switch (plan) {
            case OBJECT_STORE_AWS_PLAN:
                return createServiceInfoForAws(credentials);
            case OBJECT_STORE_AZURE_PLAN:
                return createServiceInfoForAzure(credentials);
            case OBJECT_STORE_ALICLOUD_PLAN:
                return createServiceInfoForAliCloud(credentials);
            default:
                throw new IllegalStateException("Unsupported service plan for object store!");
        }
    }

    private ObjectStoreServiceInfo createServiceInfoForAws(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get("access_key_id");
        String secretAccessKey = (String) credentials.get("secret_access_key");
        String bucket = (String) credentials.get("bucket");
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider("aws-s3")
                                              .identity(accessKeyId)
                                              .credential(secretAccessKey)
                                              .container(bucket)
                                              .build();
    }

    private ObjectStoreServiceInfo createServiceInfoForAzure(Map<String, Object> credentials) {
        String accountName = (String) credentials.get("account_name");
        String sasToken = (String) credentials.get("sas_token");
        String containerName = (String) credentials.get("container_name");
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider("azureblob")
                                              .identity(accountName)
                                              .credential(sasToken)
                                              .container(containerName)
                                              .build();
    }

    private ObjectStoreServiceInfo createServiceInfoForAliCloud(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get("access_key_id");
        String secretAccessKey = (String) credentials.get("secret_access_key");
        String bucket = (String) credentials.get("bucket");
        String region = (String) credentials.get("region");
        String endpoint = (String) credentials.get("endpoint");
        return ImmutableObjectStoreServiceInfo.builder()
                                              .provider("aliyun-oss")
                                              .identity(accessKeyId)
                                              .credential(secretAccessKey)
                                              .container(bucket)
                                              .endpoint(endpoint)
                                              .region(region)
                                              .build();
    }

}
