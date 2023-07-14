package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.cloudfoundry.multiapps.controller.core.test.MockBuilder;
import org.cloudfoundry.multiapps.controller.persistence.query.ProgressMessageQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProgressMessagesCleanerTest {

    private static final LocalDateTime EXPIRATION_TIME = LocalDateTime.ofInstant(Instant.ofEpochMilli(5000), ZoneId.systemDefault());

    @Mock
    private ProgressMessageService progressMessageService;
    @Mock(answer = Answers.RETURNS_SELF)
    private ProgressMessageQuery progressMessageQuery;
    @InjectMocks
    private ProgressMessagesCleaner cleaner;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(progressMessageService.createQuery()).thenReturn(progressMessageQuery);
        new MockBuilder<>(progressMessageQuery).on(query -> query.olderThan(EXPIRATION_TIME))
                                               .build();
    }

    @Test
    void testExecute() {
        cleaner.execute(EXPIRATION_TIME);
        verify(progressMessageService.createQuery()
                                     .olderThan(EXPIRATION_TIME)).delete();
    }

}
