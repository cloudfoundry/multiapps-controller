package org.cloudfoundry.multiapps.controller.web.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.cloudfoundry.multiapps.controller.web.configuration.bean.factory.ObjectStoreFileStorageFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

@Configuration
public class FileStorageConfiguration {

    private static final String OBJECT_STORE_SERVICE_NAME = "deploy-service-os";
    private static final long MAX_UPLOAD_SIZE = 4294967296L; // 4GB

    @Inject
    @Bean
    public ObjectStoreFileStorageFactoryBean objectStoreFileStorage(EnvironmentServicesFinder vcapServiceFinder) {
        return new ObjectStoreFileStorageFactoryBean(OBJECT_STORE_SERVICE_NAME, vcapServiceFinder);
    }

    @Bean(name = "filterMultipartResolver")
    public MultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setMaxUploadSize(MAX_UPLOAD_SIZE);
        resolver.setMaxUploadSizePerFile(MAX_UPLOAD_SIZE);
        return resolver;
    }

    @Inject
    @Bean(name = "asyncFileUploadExecutor")
    public ExecutorService asyncFileUploadExecutor(ApplicationConfiguration configuration) {
        return new ThreadPoolExecutor(5,
                                      configuration.getFilesAsyncUploadExecutorMaxThreads(),
                                      30,
                                      TimeUnit.SECONDS,
                                      new LinkedBlockingQueue<>());
    }
}
