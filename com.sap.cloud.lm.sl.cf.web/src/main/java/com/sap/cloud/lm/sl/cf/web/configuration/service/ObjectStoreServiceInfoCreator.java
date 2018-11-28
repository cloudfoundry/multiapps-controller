package com.sap.cloud.lm.sl.cf.web.configuration.service;

import java.util.Map;

import org.springframework.cloud.cloudfoundry.CloudFoundryServiceInfoCreator;
import org.springframework.cloud.cloudfoundry.Tags;

public class ObjectStoreServiceInfoCreator extends CloudFoundryServiceInfoCreator<ObjectStoreServiceInfo> {

    private static final String OBJECT_STORE_LABEL = "objectstore";
    private static final String OBJECT_STORE_AWS_PLAN = "s3-standard";
    private static final String OBJECT_STORE_INSTANCE_NAME = "deploy-service-os";

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
        return OBJECT_STORE_LABEL.equals(label) && OBJECT_STORE_INSTANCE_NAME.equals(name) && OBJECT_STORE_AWS_PLAN.equals(plan);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ObjectStoreServiceInfo createServiceInfo(Map<String, Object> serviceData) {
        Map<String, Object> credentials = (Map<String, Object>) serviceData.get("credentials");
        String accessKeyId = (String) credentials.get("access_key_id");
        String secretAccessKey = (String) credentials.get("secret_access_key");
        String bucket = (String) credentials.get("bucket");
        return createServiceInfo(accessKeyId, secretAccessKey, bucket);
    }

    private ObjectStoreServiceInfo createServiceInfo(String accessKeyId, String secretAccessKey, String bucket) {
        return new ObjectStoreServiceInfo(OBJECT_STORE_INSTANCE_NAME, accessKeyId, secretAccessKey, bucket);
    }

}
