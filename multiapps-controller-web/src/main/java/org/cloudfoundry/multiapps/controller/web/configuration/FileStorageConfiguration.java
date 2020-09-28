package org.cloudfoundry.multiapps.controller.web.configuration;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.cloudfoundry.multiapps.controller.web.configuration.bean.factory.FileSystemFileStorageFactoryBean;
import org.cloudfoundry.multiapps.controller.web.configuration.bean.factory.ObjectStoreFileStorageFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileStorageConfiguration {

    private static final String FS_STORAGE_SERVICE_NAME = "deploy-service-fss";
    private static final String OBJECT_STORE_SERVICE_NAME = "deploy-service-os";

    @Inject
    @Bean
    public FileSystemFileStorageFactoryBean fileSystemFileStorage(EnvironmentServicesFinder vcapServiceFinder) {
        return new FileSystemFileStorageFactoryBean(FS_STORAGE_SERVICE_NAME, vcapServiceFinder);
    }

    @Inject
    @Bean
    public ObjectStoreFileStorageFactoryBean objectStoreFileStorage(EnvironmentServicesFinder vcapServiceFinder) {
        return new ObjectStoreFileStorageFactoryBean(OBJECT_STORE_SERVICE_NAME, vcapServiceFinder);
    }

}
