package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.Mockito.verify;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;

class ProgressMessagesCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);

    @Mock
    private ProgressMessageService progressMessageService;
    @InjectMocks
    private ProgressMessagesCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testExecute() {
        cleaner.execute(EXPIRATION_TIME);
        verify(progressMessageService).removeOlderThan(EXPIRATION_TIME);
    }

}
