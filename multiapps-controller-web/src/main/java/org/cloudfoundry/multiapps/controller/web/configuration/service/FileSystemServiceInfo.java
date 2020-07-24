package org.cloudfoundry.multiapps.controller.web.configuration.service;

import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.service.ServiceInfo.ServiceLabel;

@ServiceLabel("fs-storage")
public class FileSystemServiceInfo extends BaseServiceInfo {

    private final String storagePath;

    public FileSystemServiceInfo(String id, String storagePath) {
        super(id);
        this.storagePath = storagePath;
    }

    public String getStoragePath() {
        return storagePath;
    }

}
