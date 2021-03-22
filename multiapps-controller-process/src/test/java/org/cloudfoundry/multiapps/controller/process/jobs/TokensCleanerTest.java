package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.ArgumentMatchers.any;

import java.util.Date;

import org.cloudfoundry.multiapps.controller.persistence.query.AccessTokenQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class TokensCleanerTest {

    @Mock
    private AccessTokenService accessTokenService;
    @InjectMocks
    private TokensCleaner cleaner;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testDeleteMethodIsInvoked() {
        AccessTokenQuery accessTokenQuery = Mockito.mock(AccessTokenQuery.class);
        Mockito.when(accessTokenService.createQuery())
               .thenReturn(accessTokenQuery);
        Mockito.when(accessTokenQuery.expiresBefore(any()))
               .thenReturn(accessTokenQuery);
        cleaner.execute(new Date());
        Mockito.verify(accessTokenQuery)
               .delete();
    }

}
