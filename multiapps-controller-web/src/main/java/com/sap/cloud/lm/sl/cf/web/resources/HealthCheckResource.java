package com.sap.cloud.lm.sl.cf.web.resources;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sap.cloud.lm.sl.cf.core.health.HealthRetriever;
import com.sap.cloud.lm.sl.cf.core.health.model.Health;
import com.sap.cloud.lm.sl.cf.core.model.CachedObject;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthCheckResource {

    private static final long CACHE_TIME_IN_SECONDS = 10;
    private static final CachedObject<Health> CACHED_RESPONSE = new CachedObject<>(CACHE_TIME_IN_SECONDS);

    @Inject
    private HealthRetriever healthRetriever;

    @GET
    public Health getHealth() {
        return CACHED_RESPONSE.get(() -> healthRetriever.getHealth());
    }

}
