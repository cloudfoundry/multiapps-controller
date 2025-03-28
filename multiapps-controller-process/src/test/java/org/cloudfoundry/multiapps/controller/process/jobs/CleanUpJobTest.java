package org.cloudfoundry.multiapps.controller.process.jobs;

import java.util.List;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CleanUpJobTest {

    @Test
    void testExecutionResilience() {
        Cleaner cleaner1 = Mockito.mock(Cleaner.class);
        Cleaner cleaner2 = Mockito.mock(Cleaner.class);
        Mockito.doThrow(new SLException("Will it work?"))
               .when(cleaner2)
               .execute(Mockito.any());
        Cleaner cleaner3 = Mockito.mock(Cleaner.class);
        List<Cleaner> cleaners = List.of(cleaner1, cleaner2, cleaner3);

        CleanUpJob cleanUpJob = createCleanUpJob(getMockedApplicationConfiguration(), cleaners);
        cleanUpJob.execute();

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

    private ApplicationConfiguration getMockedApplicationConfiguration() {
        ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(configuration.getApplicationInstanceIndex())
               .thenReturn(0);
        Mockito.when(configuration.getMaxTtlForOldData())
               .thenReturn(ApplicationConfiguration.DEFAULT_MAX_TTL_FOR_OLD_DATA);
        return configuration;
    }

}
