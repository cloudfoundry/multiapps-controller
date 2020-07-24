package com.sap.cloud.lm.sl.cf.web.resources;

import javax.inject.Inject;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.lm.sl.cf.core.health.HealthRetriever;
import com.sap.cloud.lm.sl.cf.core.health.model.Health;
import com.sap.cloud.lm.sl.cf.core.model.CachedObject;

@RestController
@RequestMapping("/public/health")
public class HealthCheckResource {

    private static final long CACHE_TIME_IN_SECONDS = 10;
    private static final CachedObject<Health> CACHED_RESPONSE = new CachedObject<>(CACHE_TIME_IN_SECONDS);

    @Inject
    private HealthRetriever healthRetriever;

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    public Health getHealth() {
        return CACHED_RESPONSE.get(healthRetriever::getHealth);
    }

}
