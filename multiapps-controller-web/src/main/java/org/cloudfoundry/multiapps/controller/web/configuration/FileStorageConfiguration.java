package org.cloudfoundry.multiapps.controller.web.configuration;

import org.cloudfoundry.multiapps.controller.web.configuration.bean.factory.FileSystemFileStorageFactoryBean;
import org.cloudfoundry.multiapps.controller.web.configuration.bean.factory.ObjectStoreFileStorageFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfiguration {

    private static final String FS_STORAGE_SERVICE_NAME = "deploy-service-fss";
    private static final String OBJECT_STORE_SERVICE_NAME = "deploy-service-os";

    @Bean
    public FileSystemFileStorageFactoryBean fileSystemFileStorage() {
        return new FileSystemFileStorageFactoryBean(FS_STORAGE_SERVICE_NAME);
    }

    @Bean
    public ObjectStoreFileStorageFactoryBean objectStoreFileStorage() {
        return new ObjectStoreFileStorageFactoryBean(OBJECT_STORE_SERVICE_NAME);
    }
}
