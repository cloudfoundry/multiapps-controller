package com.sap.cloud.lm.sl.cf.web.configuration.service;

import java.util.Map;

import org.springframework.cloud.cloudfoundry.CloudFoundryServiceInfoCreator;
import org.springframework.cloud.cloudfoundry.Tags;

public class ObjectStoreServiceInfoCreator extends CloudFoundryServiceInfoCreator<ObjectStoreServiceInfo> {

    private static final String OBJECT_STORE_LABEL = "objectstore";
    private static final String OBJECT_STORE_INSTANCE_NAME = "deploy-service-os";
    private static final String OBJECT_STORE_AWS_PLAN = "s3-standard";
    private static final String OBJECT_STORE_AZURE_PLAN = "azure-standard";
    private static final String OBJECT_STORE_ALICLOUD_PLAN = "oss-standard";

    public ObjectStoreServiceInfoCreator() {
        super(new Tags(OBJECT_STORE_LABEL), "");
    }

    @Override
    public boolean accept(Map<String, Object> serviceData) {
        return serviceMatches(serviceData);
    }

    private boolean serviceMatches(Map<String, Object> serviceData) {
        String label = (String) serviceData.get("label");
        String name = (String) serviceData.get("name");
        String plan = (String) serviceData.get("plan");
        return OBJECT_STORE_LABEL.equals(label) && OBJECT_STORE_INSTANCE_NAME.equals(name) && planMatches(plan);
    }

    private boolean planMatches(String plan) {
        return OBJECT_STORE_AWS_PLAN.equals(plan) || OBJECT_STORE_AZURE_PLAN.equals(plan) || OBJECT_STORE_ALICLOUD_PLAN.equals(plan);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ObjectStoreServiceInfo createServiceInfo(Map<String, Object> serviceData) {
        String plan = (String) serviceData.get("plan");
        Map<String, Object> credentials = (Map<String, Object>) serviceData.get("credentials");
        if (plan.equals(OBJECT_STORE_AWS_PLAN)) {
            return createServiceInfoForAws(credentials);
        } else if (plan.equals(OBJECT_STORE_AZURE_PLAN)) {
            return createServiceInfoForAzure(credentials);
        } else if (plan.equals(OBJECT_STORE_ALICLOUD_PLAN)) {
            return createServiceInfoForAliCloud(credentials);
        }
        throw new IllegalStateException("Unsupported service plan for object store!");

    }

    private ObjectStoreServiceInfo createServiceInfoForAws(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get("access_key_id");
        String secretAccessKey = (String) credentials.get("secret_access_key");
        String bucket = (String) credentials.get("bucket");
        return ObjectStoreServiceInfo.builder()
                                     .id(OBJECT_STORE_INSTANCE_NAME)
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
        return ObjectStoreServiceInfo.builder()
                                     .id(OBJECT_STORE_INSTANCE_NAME)
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
        return ObjectStoreServiceInfo.builder()
                                     .id(OBJECT_STORE_INSTANCE_NAME)
                                     .provider("aliyun-oss")
                                     .identity(accessKeyId)
                                     .credential(secretAccessKey)
                                     .container(bucket)
                                     .endpoint(endpoint)
                                     .region(region)
                                     .build();
    }

}
