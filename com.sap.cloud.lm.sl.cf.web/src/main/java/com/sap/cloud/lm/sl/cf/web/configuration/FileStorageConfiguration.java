package com.sap.cloud.lm.sl.cf.web.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.sap.cloud.lm.sl.cf.web.configuration.bean.factory.FileSystemFileStorageFactoryBean;
import com.sap.cloud.lm.sl.cf.web.configuration.bean.factory.ObjectStoreFileStorageFactoryBean;

@Configuration
public class FileStorageConfiguration {

    private static final String FS_STORAGE_SERVICE_NAME = "deploy-service-fss";
    private static final String OBJECT_STORE_SERVICE_NAME = "deploy-service-os";

    @Bean
    public FileSystemFileStorageFactoryBean fileSystemFileStorage() {
        return new FileSystemFileStorageFactoryBean(FS_STORAGE_SERVICE_NAME);
    }

    @Bean
    @Profile("cf")
    public ObjectStoreFileStorageFactoryBean objectStoreFileStorage() {
        return new ObjectStoreFileStorageFactoryBean(OBJECT_STORE_SERVICE_NAME);
    }
}
