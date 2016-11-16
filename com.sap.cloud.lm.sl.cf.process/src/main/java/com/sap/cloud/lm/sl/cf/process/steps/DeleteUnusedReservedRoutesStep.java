package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.OccupiedPortsDetector;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("deleteUnusedReservedRoutesStep")
public class DeleteUnusedReservedRoutesStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteUnusedReservedRoutesStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("deleteUnusedReservedRoutesTask", "Delete Unused Reserved Ports", "Delete Unused Reserved Ports");
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.DELETING_UNUSED_RESERVED_ROUTES, LOGGER);
            boolean portBasedRouting = ContextUtil.getVariable(context, Constants.VAR_PORT_BASED_ROUTING, false);

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);
            List<CloudApplicationExtended> apps = StepsUtil.getAppsToDeploy(context);
            String defaultDomain = getDefaultDomain(context);

            if (portBasedRouting) {
                PortAllocator portAllocator = clientProvider.getPortAllocator(client, defaultDomain);
                portAllocator.setAllocatedPorts(StepsUtil.getAllocatedPorts(context));

                Set<Integer> applicationPorts = getApplicationPorts(apps);
                debug(context, format(Messages.APPLICATION_PORTS, applicationPorts), LOGGER);
                portAllocator.freeAllExcept(applicationPorts);
                StepsUtil.setAllocatedPorts(context, applicationPorts);
                debug(context, format(Messages.ALLOCATED_PORTS, portAllocator.getAllocatedPorts()), LOGGER);
            }

            debug(context, Messages.UNUSED_RESERVED_ROUTES_DELETED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_DELETING_UNUSED_RESERVED_ROUTES, e, LOGGER);
            throw e;
        }
    }

    private String getDefaultDomain(DelegateExecution context) throws SLException {
        Map<String, Object> xsPlaceholderReplacementValues = StepsUtil.getXsPlaceholderReplacementValues(context);
        return (String) xsPlaceholderReplacementValues.get(SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER);
    }

    private Set<Integer> getApplicationPorts(List<CloudApplicationExtended> apps) {
        Map<String, List<Integer>> occupiedPorts = new OccupiedPortsDetector().detectOccupiedPorts(ListUtil.upcastUnmodifiable(apps));
        Set<Integer> result = new TreeSet<>();
        for (String applicationName : occupiedPorts.keySet()) {
            result.addAll(occupiedPorts.get(applicationName));
        }
        return result;
    }

}
