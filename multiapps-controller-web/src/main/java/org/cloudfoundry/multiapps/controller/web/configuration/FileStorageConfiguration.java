package org.cloudfoundry.multiapps.controller.web.configuration;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorage;
import org.cloudfoundry.multiapps.controller.persistence.services.resilience.NoRetryErrorClassifier;
import org.cloudfoundry.multiapps.controller.persistence.services.resilience.RetryableErrorClassifier;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.cloudfoundry.multiapps.controller.web.configuration.factory.ObjectStoreSelectorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
public class FileStorageConfiguration {

    private static final String OBJECT_STORE_SERVICE_NAME = "deploy-service-os";

    @Bean
    public ObjectStoreSelectorFactory objectStoreSelector(EnvironmentServicesFinder vcapServiceFinder,
                                                          ApplicationConfiguration applicationConfiguration) {
        return new ObjectStoreSelectorFactory(OBJECT_STORE_SERVICE_NAME, vcapServiceFinder, applicationConfiguration);
    }

    @Bean
    public FileStorage fileStorage(ObjectStoreSelectorFactory selector) {
        return selector.fileStorage();
    }

    @Bean
    public RetryableErrorClassifier retryableErrorClassifier(ObjectStoreSelectorFactory selector) {
        RetryableErrorClassifier classifier = selector.classifier();
        return classifier != null ? classifier : new NoRetryErrorClassifier();
    }

    @Bean(name = "filterMultipartResolver")
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
}
