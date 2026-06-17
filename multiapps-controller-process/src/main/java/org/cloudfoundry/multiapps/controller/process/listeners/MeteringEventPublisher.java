package org.cloudfoundry.multiapps.controller.process.listeners;

import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.metering.client.MeteringClient;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class MeteringEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringEventPublisher.class);

    private final MeteringClient meteringClient;
    private final ApplicationConfiguration configuration;

    @Inject
    public MeteringEventPublisher(MeteringClient meteringClient, ApplicationConfiguration configuration) {
        this.meteringClient = meteringClient;
        this.configuration = configuration;
    }

    public void publishStarted(DelegateExecution execution, ProcessType processType) {
        publish(execution, processType, "deploy-started");
    }

    public void publishFinalState(DelegateExecution execution, ProcessType processType, Operation.State state) {
        publish(execution, processType, state == Operation.State.FINISHED ? "deploy-finished" : "deploy-aborted");
    }

    private void publish(DelegateExecution execution, ProcessType processType, String measureId) {
        String organisationId = VariableHandling.get(execution, Variables.ORGANIZATION_GUID);
        if (organisationId == null) {
            LOGGER.debug("Skipping metering event {}: organization GUID is not set on the execution", measureId);
            return;
        }
        String region = configuration.getCfRegion();
        if (region == null || region.isBlank()) {
            LOGGER.debug("Skipping metering event {}: CF_REGION env var is not set", measureId);
            return;
        }
        String serviceId = configuration.getMeteringServiceId();
        String servicePlan = configuration.getMeteringServicePlan();
        if (serviceId == null || serviceId.isBlank() || servicePlan == null || servicePlan.isBlank()) {
            LOGGER.debug("Skipping metering event {}: METERING_SERVICE_ID or METERING_SERVICE_PLAN env var is not set", measureId);
            return;
        }
        String dimension = processType != null ? processType.getName()
                                                            .toLowerCase(Locale.ROOT)
                                                : "unknown";
        meteringClient.recordUsage(region, organisationId, List.of(dimension), measureId, serviceId, servicePlan);
    }
}
