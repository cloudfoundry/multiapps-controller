package com.sap.cloud.lm.sl.cf.web.configuration.bean.factory;

import java.text.MessageFormat;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.services.FileSystemFileService;
import com.sap.cloud.lm.sl.cf.web.configuration.service.FileSystemServiceInfo;
import com.sap.cloud.lm.sl.cf.web.message.Messages;

public class FileSystemFileServiceFactoryBean implements FactoryBean<FileSystemFileService>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemFileServiceFactoryBean.class);

    private String serviceName;
    private DataSourceWithDialect dataSourceWithDialect;
    private FileSystemFileService fileSystemFileService;

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setDataSourceWithDialect(DataSourceWithDialect dataSourceWithDialect) {
        this.dataSourceWithDialect = dataSourceWithDialect;
    }

    @Override
    public void afterPropertiesSet() {
        String storagePath = getStoragePath(serviceName);
        this.fileSystemFileService = createFileSystemFileService(storagePath);
    }

    private FileSystemFileService createFileSystemFileService(String storagePath) {
        return storagePath == null ? null : new FileSystemFileService(dataSourceWithDialect, storagePath);
    }

    @Override
    public FileSystemFileService getObject() {
        return fileSystemFileService;
    }

    @Override
    public Class<?> getObjectType() {
        return FileSystemFileService.class;
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
