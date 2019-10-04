package com.sap.cloud.lm.sl.cf.web.configuration.service;

import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.service.ServiceInfo.ServiceLabel;

@ServiceLabel("objectstore")
public class ObjectStoreServiceInfo extends BaseServiceInfo {

    private final String accessKeyId;
    private final String secretAccessKey;
    private final String bucket;

    public ObjectStoreServiceInfo(String id, String accessKeyId, String secretAccessKey, String bucket) {
        super(id);
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.bucket = bucket;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getBucket() {
        return bucket;
    }
}
