package org.cloudfoundry.multiapps.controller.process.security.store;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.services.SecretTokenService;

@Named
public class SecretTokenStoreFactory {

    private SecretTokenService secretTokenService;

    @Inject
    public SecretTokenStoreFactory(SecretTokenService secretTokenService) {
        this.secretTokenService = secretTokenService;
    }

    public SecretTokenStore createSecretTokenStore(String encryptionKey, String encryptionKeyId) {
        return new SecretTokenStoreImpl(secretTokenService, encryptionKey, encryptionKeyId);
    }

    public SecretTokenStoreDeletion createSecretTokenStoreDeletionRelated() {
        return new SecretTokenStoreImplWithoutKey(secretTokenService);
    }

}
