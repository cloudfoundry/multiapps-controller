package com.sap.cloud.lm.sl.cf.web.resources;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sap.cloud.lm.sl.cf.core.health.HealthRetriever;
import com.sap.cloud.lm.sl.cf.core.health.model.Health;

@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthCheckResource {

    @Inject
    private HealthRetriever healthRetriever;

    @GET
    public Health getHealthCheckOperations() {
        return healthRetriever.getHealth();
    }

}
