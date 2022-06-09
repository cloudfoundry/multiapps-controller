package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;

class TokensCleanerTest {

    @Mock
    private AccessTokenDao accessTokenDao;
    @InjectMocks
    private TokensCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testExecute() {
        cleaner.execute(null);
        verify(accessTokenDao).deleteTokensWithExpirationBefore(any());
    }

}
