package com.sap.cloud.lm.sl.cf.web.ds;

import java.util.List;
import java.util.Map;

import org.springframework.cloud.cloudfoundry.CloudFoundryServiceInfoCreator;
import org.springframework.cloud.cloudfoundry.Tags;

public class FileServiceInfoCreator extends CloudFoundryServiceInfoCreator<FileSystemServiceInfo> {

    private static final String DEFAULT_FILE_SERVICE_LABEL = "fs-storage";
    private static final String DEFAULT_FILE_SERVICE_ID = "deploy-service-fss";

    public FileServiceInfoCreator() {
        super(new Tags(DEFAULT_FILE_SERVICE_LABEL), "");
    }

    @Override
    public boolean accept(Map<String, Object> serviceData) {
        return serviceMatches(serviceData);
    }

    private boolean serviceMatches(Map<String, Object> serviceData) {
        String label = (String) serviceData.get("label");
        String name = (String) serviceData.get("name");
        return DEFAULT_FILE_SERVICE_LABEL.equals(label) && DEFAULT_FILE_SERVICE_ID.equals(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public FileSystemServiceInfo createServiceInfo(Map<String, Object> serviceData) {
        List<Object> volumeMounts = (List<Object>) serviceData.get("volume_mounts");
        Map<String, Object> volumeMount = (Map<String, Object>) volumeMounts.get(0);
        String storagePath = (String) volumeMount.get("container_dir");
        return createServiceInfo(storagePath);
    }

    private FileSystemServiceInfo createServiceInfo(String storagePath) {
        return new FileSystemServiceInfo(DEFAULT_FILE_SERVICE_ID, storagePath);
    }

}
