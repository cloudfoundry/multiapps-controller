package com.sap.cloud.lm.sl.cf.web.servicebroker;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogConfiguration {

    @Named
    public Catalog catalog() {
        return Catalog.builder()
                      .serviceDefinitions(getServiceDefinition())
                      .build();
    }

    private ServiceDefinition getServiceDefinition() {
        return ServiceDefinition.builder()
                                .id("2de182af-15a7-337e-88f8-d7bae8ebd1aa")
                                .name("mta-deployer")
                                .description("Allows deployment of multitarget applications (MTAs).")
                                .bindable(false)
                                .plans(getPlans())
                                .build();
    }

    private List<Plan> getPlans() {
        Plan standardPlan = Plan.builder()
                                .id("72f11a0e-f31e-3f40-be70-3f83579acc7f")
                                .name("standard")
                                .description("Allows deployment of multitarget applications (MTAs) with all released non-beta features.")
                                .build();
        return Arrays.asList(standardPlan);
    }

}
