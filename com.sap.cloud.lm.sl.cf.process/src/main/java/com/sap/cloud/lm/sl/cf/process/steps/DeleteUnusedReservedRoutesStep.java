package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.OccupiedPortsDetector;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("deleteUnusedReservedRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteUnusedReservedRoutesStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.DELETING_UNUSED_RESERVED_ROUTES);
            boolean portBasedRouting = StepsUtil.getVariableOrDefault(execution.getContext(), Constants.VAR_PORT_BASED_ROUTING, false);

            List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(execution.getContext());
            String defaultDomain = getDefaultDomain(execution.getContext());

            if (portBasedRouting) {
                PortAllocator portAllocator = clientProvider.getPortAllocator(execution.getXsControllerClient(), defaultDomain);
                portAllocator.setAllocatedPorts(StepsUtil.getAllocatedPorts(execution.getContext()));

                Set<Integer> applicationPorts = getApplicationPorts(apps);
                getStepLogger().debug(Messages.APPLICATION_PORTS, applicationPorts);
                portAllocator.freeAllExcept(applicationPorts);
                StepsUtil.setAllocatedPorts(execution.getContext(), portAllocator.getAllocatedPorts());
                getStepLogger().debug(Messages.ALLOCATED_PORTS, portAllocator.getAllocatedPorts());
            }

            getStepLogger().debug(Messages.UNUSED_RESERVED_ROUTES_DELETED);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DELETING_UNUSED_RESERVED_ROUTES);
            throw e;
        }
    }

    private String getDefaultDomain(DelegateExecution context) {
        Map<String, Object> xsPlaceholderReplacementValues = StepsUtil.getXsPlaceholderReplacementValues(context);
        return (String) xsPlaceholderReplacementValues.get(SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER);
    }

    private Set<Integer> getApplicationPorts(List<CloudApplicationExtended> apps) {
        OccupiedPortsDetector occupiedPortsDetector = new OccupiedPortsDetector();
        return apps.stream()
            .flatMap(app -> occupiedPortsDetector.detectOccupiedPorts(app)
                .stream())
            .collect(Collectors.toCollection(TreeSet::new));
    }

}
