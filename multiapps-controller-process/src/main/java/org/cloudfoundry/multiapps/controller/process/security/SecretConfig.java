package org.cloudfoundry.multiapps.controller.process.security;

import java.security.Security;
import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerializerConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.SecretTokenService;
import org.cloudfoundry.multiapps.controller.process.security.resolver.SecretTokenKeyResolver;
import org.cloudfoundry.multiapps.controller.process.security.resolver.SecretTokenKeyResolverImpl;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStoreFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecretConfig {

    @Bean
    public SecretTokenKeyResolver secretTokenKeyResolver() {
        return new SecretTokenKeyResolverImpl();
    }

    @Bean
    public SecretTokenStoreFactory secretTokenStoreFactory(SecretTokenService secretTokenService) {
        return new SecretTokenStoreFactory(secretTokenService);
    }

    @Bean
    public SecretTransformationStrategy secretTransformationStrategy() {
        SecureSerializerConfiguration secureSerializerConfiguration = new SecureSerializerConfiguration();
        Set<String> secretNames = new HashSet<>(secureSerializerConfiguration.getSensitiveElementNames());

        return new SecretTransformationStrategyImpl(secretNames);
    }

    @PostConstruct
    public void addBouncyCastleSecureProvider() {
        if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleFipsProvider());
        }
    }

}
