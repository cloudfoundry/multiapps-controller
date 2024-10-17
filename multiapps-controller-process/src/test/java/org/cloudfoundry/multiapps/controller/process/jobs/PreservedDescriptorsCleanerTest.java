package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.cloudfoundry.multiapps.controller.persistence.query.DescriptorPreserverQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.DescriptorPreserverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PreservedDescriptorsCleanerTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final LocalDateTime DATE = LocalDateTime.parse("2024-12-05T13:30:25.010Z", DATE_TIME_FORMATTER);

    @Mock
    private DescriptorPreserverService descriptorPreserverService;
    @Mock
    private DescriptorPreserverQuery descriptorPreserverQuery;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        ;
    }

    @Test
    void testCleanup() {
        when(descriptorPreserverQuery.olderThan(DATE)).thenReturn(descriptorPreserverQuery);
        when(descriptorPreserverService.createQuery()).thenReturn(descriptorPreserverQuery);

        PreservedDescriptorsCleaner cleaner = new PreservedDescriptorsCleaner(descriptorPreserverService);

        cleaner.execute(DATE);

        verify(descriptorPreserverQuery).delete();
    }
}
