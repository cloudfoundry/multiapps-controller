package org.cloudfoundry.multiapps.controller.process.security;

import java.security.Security;

import jakarta.annotation.PostConstruct;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecretConfig {

    @PostConstruct
    public void addBouncyCastleSecureProvider() {
        if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleFipsProvider());
        }
    }

}
