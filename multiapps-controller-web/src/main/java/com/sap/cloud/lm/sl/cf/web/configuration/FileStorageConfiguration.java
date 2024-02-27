package com.sap.cloud.lm.sl.cf.web.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sap.cloud.lm.sl.cf.web.configuration.bean.factory.FileSystemFileStorageFactoryBean;

@Configuration
public class FileStorageConfiguration {

    private static final String FS_STORAGE_SERVICE_NAME = "deploy-service-fss";

    @Bean
    public FileSystemFileStorageFactoryBean fileSystemFileStorage() {
        return new FileSystemFileStorageFactoryBean(FS_STORAGE_SERVICE_NAME);
    }

}
