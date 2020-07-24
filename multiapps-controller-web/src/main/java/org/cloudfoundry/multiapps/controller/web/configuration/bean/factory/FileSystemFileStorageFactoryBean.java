package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.persistence.services.FileSystemFileStorage;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.configuration.service.FileSystemServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;

public class FileSystemFileStorageFactoryBean implements FactoryBean<FileSystemFileStorage>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemFileStorageFactoryBean.class);

    private final String serviceName;
    private FileSystemFileStorage fileSystemFileStorage;

    public FileSystemFileStorageFactoryBean(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public void afterPropertiesSet() {
        String storagePath = getStoragePath(serviceName);
        this.fileSystemFileStorage = createFileSystemFileStorage(storagePath);
    }

    private FileSystemFileStorage createFileSystemFileStorage(String storagePath) {
        return storagePath == null ? null : new FileSystemFileStorage(storagePath);
    }

    @Override
    public FileSystemFileStorage getObject() {
        return fileSystemFileStorage;
    }

    @Override
    public Class<?> getObjectType() {
        return FileSystemFileStorage.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private String getStoragePath(String serviceName) {
        if (StringUtils.isEmpty(serviceName)) {
            LOGGER.warn(Messages.FILE_SYSTEM_SERVICE_NAME_IS_NOT_SPECIFIED);
            return null;
        }
        try {
            CloudFactory cloudFactory = new CloudFactory();
            Cloud cloud = cloudFactory.getCloud();
            FileSystemServiceInfo serviceInfo = (FileSystemServiceInfo) cloud.getServiceInfo(serviceName);
            return serviceInfo.getStoragePath();
        } catch (CloudException e) {
            LOGGER.warn(MessageFormat.format(Messages.FAILED_TO_DETECT_FILE_SERVICE_STORAGE_PATH, serviceName), e);
        }
        return null;
    }

}
