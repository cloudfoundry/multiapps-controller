package com.sap.cloud.lm.sl.cf.web.ds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;

import com.sap.cloud.lm.sl.cf.web.service.FileSystemServiceInfo;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;
import com.sap.cloud.lm.sl.persistence.services.DatabaseFileService;
import com.sap.cloud.lm.sl.persistence.services.FileSystemFileService;

public class FileServiceFactoryBean implements FactoryBean<AbstractFileService>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceFactoryBean.class);

    private String serviceName;
    private DatabaseFileService fileDatabaseService;
    private FileSystemFileService fileSystemFileService;
    private String storagePath;

    public void setFileDatabaseService(DatabaseFileService fileDatabaseService) {
        this.fileDatabaseService = fileDatabaseService;
    }

    public void setFileSystemFileService(FileSystemFileService fileSystemFileService) {
        this.fileSystemFileService = fileSystemFileService;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        storagePath = getFileServiceStorageLocation(serviceName);
    }

    @Override
    public AbstractFileService getObject() throws Exception {
        if (storagePath == null) {
            return fileDatabaseService;
        }
        fileSystemFileService.setStoragePath(storagePath);
        return fileSystemFileService;
    }

    @Override
    public Class<?> getObjectType() {
        return AbstractFileService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private String getFileServiceStorageLocation(String serviceName) {
        try {
            if (serviceName != null && !serviceName.isEmpty()) {
                CloudFactory cloudFactory = new CloudFactory();
                Cloud cloud = cloudFactory.getCloud();
                FileSystemServiceInfo serviceInfo = (FileSystemServiceInfo) cloud.getServiceInfo(serviceName);
                return serviceInfo.getStoragePath();
            }
        } catch (CloudException e) {
            LOGGER.debug("Persistent Shared File System Service detection failed", e);
        }
        return null;
    }

}
