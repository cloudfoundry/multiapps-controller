package com.sap.cloud.lm.sl.cf.web.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.cloud.lm.sl.cf.web.configuration.bean.factory.FileSystemFileStorageFactoryBean;
import com.sap.cloud.lm.sl.cf.web.configuration.bean.factory.ObjectStoreFileStorageFactoryBean;

@Configuration
public class FileStorageConfiguration {

    private static final String FILE_SERVICE_NAME = "deploy-service-fss";
    private static final String OBJECT_STORE_NAME = "deploy-service-os";

    @Bean(name = "fileSystemFileStorage")
    public FileSystemFileStorageFactoryBean getFileSystemFileStorageFactoryBean() {
        return new FileSystemFileStorageFactoryBean(FILE_SERVICE_NAME);
    }

    @Bean(name = "objectStoreFileStorage")
    public ObjectStoreFileStorageFactoryBean getObjectStoreFileStorageFactoryBean() {
        return new ObjectStoreFileStorageFactoryBean(OBJECT_STORE_NAME);
    }

}
