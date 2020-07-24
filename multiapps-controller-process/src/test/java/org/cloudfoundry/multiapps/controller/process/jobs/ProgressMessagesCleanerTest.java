package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.cloudfoundry.multiapps.controller.core.persistence.query.ProgressMessageQuery;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.core.util.MockBuilder;
import org.cloudfoundry.multiapps.controller.process.jobs.ProgressMessagesCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProgressMessagesCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);

    @Mock
    private ProgressMessageService progressMessageService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ProgressMessageQuery progressMessageQuery;
    @InjectMocks
    private ProgressMessagesCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        when(progressMessageService.createQuery()).thenReturn(progressMessageQuery);
        new MockBuilder<>(progressMessageQuery).on(query -> query.olderThan(EXPIRATION_TIME))
                                               .build();
    }

    @Test
    public void testExecute() {
        cleaner.execute(EXPIRATION_TIME);
        verify(progressMessageService.createQuery()
                                     .olderThan(EXPIRATION_TIME)).delete();
    }

}
