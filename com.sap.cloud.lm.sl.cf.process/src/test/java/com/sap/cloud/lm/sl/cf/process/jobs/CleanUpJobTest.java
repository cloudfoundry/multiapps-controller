package com.sap.cloud.lm.sl.cf.process.jobs;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.SLException;

public class CleanUpJobTest {

    @Test
    public void testExecutionResilience() {
        Cleaner cleaner1 = Mockito.mock(Cleaner.class);
        Cleaner cleaner2 = Mockito.mock(Cleaner.class);
        Mockito.doThrow(new SLException("Will it work?"))
               .when(cleaner2)
               .execute(Mockito.any());
        Cleaner cleaner3 = Mockito.mock(Cleaner.class);
        List<Cleaner> cleaners = Arrays.asList(cleaner1, cleaner2, cleaner3);

        CleanUpJob cleanUpJob = createCleanUpJob(new ApplicationConfiguration(), cleaners);
        cleanUpJob.execute(null);

        Mockito.verify(cleaner1)
               .execute(Mockito.any());
        Mockito.verify(cleaner2)
               .execute(Mockito.any());
        // Makes sure that all cleaners are executed even if the ones before them failed.
        Mockito.verify(cleaner3)
               .execute(Mockito.any());
    }

    private CleanUpJob createCleanUpJob(ApplicationConfiguration applicationConfiguration, List<Cleaner> cleaners) {
        CleanUpJob cleanUpJob = new CleanUpJob();
        cleanUpJob.configuration = applicationConfiguration;
        cleanUpJob.cleaners = cleaners;
        return cleanUpJob;
    }

}
