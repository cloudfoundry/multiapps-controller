package org.cloudfoundry.multiapps.controller.web.resources;

import org.cloudfoundry.multiapps.controller.core.application.health.ApplicationHealthCalculator;
import org.cloudfoundry.multiapps.controller.core.application.health.model.ApplicationHealthResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

class ApplicationHealthResourceTest {

    @Mock
    private ApplicationHealthCalculator applicationHealthCalculator;
    @Mock
    private ApplicationHealthResult applicationHealthResult;

    private ApplicationHealthResource resource;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        resource = new ApplicationHealthResource(applicationHealthCalculator);
    }

    @Test
    void testCalculateApplicationHealthDelegatesToCalculator() {
        ResponseEntity<ApplicationHealthResult> expected = ResponseEntity.ok(applicationHealthResult);
        Mockito.when(applicationHealthCalculator.calculateApplicationHealth())
               .thenReturn(expected);

        ResponseEntity<ApplicationHealthResult> result = resource.calculateApplicationHealth();

        Assertions.assertSame(expected, result);
        Mockito.verify(applicationHealthCalculator)
               .calculateApplicationHealth();
    }
}
