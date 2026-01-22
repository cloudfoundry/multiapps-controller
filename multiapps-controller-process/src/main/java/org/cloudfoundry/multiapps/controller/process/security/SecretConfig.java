package org.cloudfoundry.multiapps.controller.process.security;

import java.security.Security;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.SecretTokenService;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStoreFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecretConfig {

    @Bean
    public SecretTokenStoreFactory secretTokenStoreFactory(SecretTokenService secretTokenService) {
        return new SecretTokenStoreFactory(secretTokenService);
    }

    @PostConstruct
    public void addBouncyCastleSecureProvider() {
        if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleFipsProvider());
        }
    }

}
