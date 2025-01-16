package org.cloudfoundry.multiapps.controller.web.resources;

import org.cloudfoundry.multiapps.controller.core.application.health.ApplicationHealthCalculator;
import org.cloudfoundry.multiapps.controller.core.application.health.model.ApplicationHealthResult;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.inject.Inject;

@RestController
@RequestMapping(value = Constants.Resources.APPLICATION_HEALTH)
public class ApplicationHealthResource {

    private final ApplicationHealthCalculator applicationHealthCalculator;

    @Inject
    public ApplicationHealthResource(ApplicationHealthCalculator applicationHealthCalculator) {
        this.applicationHealthCalculator = applicationHealthCalculator;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApplicationHealthResult> calculateApplicationHealth() {
        return applicationHealthCalculator.calculateApplicationHealth();
    }
}
