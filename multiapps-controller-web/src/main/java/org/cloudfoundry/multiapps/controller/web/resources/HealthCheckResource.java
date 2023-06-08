package org.cloudfoundry.multiapps.controller.web.resources;

import java.time.Duration;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.health.HealthRetriever;
import org.cloudfoundry.multiapps.controller.core.health.model.Health;
import org.cloudfoundry.multiapps.controller.core.model.CachedObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/health")
public class HealthCheckResource {

    private static final Duration CACHE_TIME = Duration.ofSeconds(10);
    private static final CachedObject<Health> CACHED_RESPONSE = new CachedObject<>(CACHE_TIME);

    @Inject
    private HealthRetriever healthRetriever;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Health getHealth() {
        return CACHED_RESPONSE.getOrRefresh(healthRetriever::getHealth);
    }

}
