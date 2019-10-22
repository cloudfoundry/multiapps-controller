package com.sap.cloud.lm.sl.cf.web.configuration.service;

import java.util.Map;

import org.springframework.cloud.cloudfoundry.CloudFoundryServiceInfoCreator;
import org.springframework.cloud.cloudfoundry.Tags;

public class ObjectStoreServiceInfoCreator extends CloudFoundryServiceInfoCreator<ObjectStoreServiceInfo> {

    private static final String OBJECT_STORE_LABEL = "objectstore";
    private static final String OBJECT_STORE_INSTANCE_NAME = "deploy-service-os";
    private static final String OBJECT_STORE_AWS_PLAN = "s3-standard";
    private static final String OBJECT_STORE_AZURE_PLAN = "azure-standard";

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
        return OBJECT_STORE_AWS_PLAN.equals(plan) || OBJECT_STORE_AZURE_PLAN.equals(plan);
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
        }
        throw new IllegalStateException("Unsupported service plan for object store!");

    }

    private ObjectStoreServiceInfo createServiceInfoForAws(Map<String, Object> credentials) {
        String accessKeyId = (String) credentials.get("access_key_id");
        String secretAccessKey = (String) credentials.get("secret_access_key");
        String bucket = (String) credentials.get("bucket");
        String region = (String) credentials.get("region");
        String host = (String) credentials.get("host");
//<<<<<<< HEAD
        return new ObjectStoreServiceInfo(OBJECT_STORE_INSTANCE_NAME, "aws-s3", accessKeyId, secretAccessKey, bucket, region, host);
    }

    private ObjectStoreServiceInfo createServiceInfoForAzure(Map<String, Object> credentials) {
        String accountName = (String) credentials.get("account_name");
        String sasToken = (String) credentials.get("sas_token");
        String containerName = (String) credentials.get("container_name");
        return new ObjectStoreServiceInfo(OBJECT_STORE_INSTANCE_NAME, "azureblob", accountName, sasToken, containerName, null, null);
//=======
//        String region = (String) credentials.get("region");
//        String host = (String) credentials.get("host");
//        return createServiceInfo(accessKeyId, secretAccessKey, bucket, region, host);
//    }
//
//    private ObjectStoreServiceInfo createServiceInfo(String accessKeyId, String secretAccessKey, String bucket, String region, String host) {
//        return new ObjectStoreServiceInfo(OBJECT_STORE_INSTANCE_NAME, accessKeyId, secretAccessKey, bucket, region, host);
//>>>>>>> 14567d69... Fix objectstore
    }

}
