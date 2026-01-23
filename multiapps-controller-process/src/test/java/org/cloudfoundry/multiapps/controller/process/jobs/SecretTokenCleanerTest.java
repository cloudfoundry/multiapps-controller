package org.cloudfoundry.multiapps.controller.process.jobs;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStoreDeletion;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStoreFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecretTokenCleanerTest {

    private static final LocalDateTime EXPIRATION_TIME = LocalDateTime.ofInstant(Instant.ofEpochMilli(5000), ZoneId.systemDefault());

    @Mock
    private SecretTokenStoreFactory secretTokenStoreFactory;

    @Mock
    private SecretTokenStoreDeletion secretTokenStoreDeletion;

    @InjectMocks
    private SecretTokenCleaner cleaner;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(secretTokenStoreFactory.createSecretTokenStoreDeletionRelated()).thenReturn(secretTokenStoreDeletion);
        when(secretTokenStoreDeletion.deleteOlderThan(EXPIRATION_TIME)).thenReturn(5);
    }

    @Test
    void testExecute() {
        cleaner.execute(EXPIRATION_TIME);

        verify(secretTokenStoreFactory).createSecretTokenStoreDeletionRelated();
        verify(secretTokenStoreDeletion).deleteOlderThan(EXPIRATION_TIME);
    }
}
