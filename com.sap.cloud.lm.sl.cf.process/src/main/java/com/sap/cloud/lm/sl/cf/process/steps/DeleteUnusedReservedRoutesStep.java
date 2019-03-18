package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.ModuleToDeployHelper;
import com.sap.cloud.lm.sl.cf.core.helpers.OccupiedPortsDetector;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

@Component("deleteUnusedReservedRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteUnusedReservedRoutesStep extends SyncFlowableStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.DELETING_UNUSED_RESERVED_ROUTES);
            boolean portBasedRouting = StepsUtil.getBoolean(execution.getContext(), Constants.VAR_PORT_BASED_ROUTING, false);

            String defaultDomain = getDefaultDomain(execution.getContext());

            if (portBasedRouting) {
                PortAllocator portAllocator = clientProvider.getPortAllocator(execution.getXsControllerClient(), defaultDomain);
                portAllocator.setAllocatedPorts(StepsUtil.getAllocatedPorts(execution.getContext()));

                Map<String, Set<Integer>> usedApplicationPorts = getUsedApplicationPorts(execution.getContext());
                getStepLogger().debug(Messages.USED_APPLICATION_PORTS, usedApplicationPorts);
                freeUnusedPortsAllocatedForModulesToDeploy(execution.getContext(), portAllocator, usedApplicationPorts);

                freeUnusedPortsForNotDeployedModules(execution.getContext(), portAllocator, usedApplicationPorts);
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

    private void freeUnusedPortsAllocatedForModulesToDeploy(DelegateExecution context, PortAllocator portAllocator,
        Map<String, Set<Integer>> usedApplicationPorts) {
        for (String module : usedApplicationPorts.keySet()) {
            portAllocator.freeUnusedForModule(module, usedApplicationPorts.get(module));
        }
    }

    private void freeUnusedPortsForNotDeployedModules(DelegateExecution context, PortAllocator portAllocator,
        Map<String, Set<Integer>> usedApplicationPorts) {
        Set<String> allMtaModules = StepsUtil.getMtaModules(context);
        allMtaModules.removeAll(usedApplicationPorts.keySet());
        // Remove all allocated ports for modules which are not marked for deploy
        for (String module : allMtaModules) {
            portAllocator.freeUnusedForModule(module, Collections.emptySet());
        }
    }

    private String getDefaultDomain(DelegateExecution context) {
        Map<String, Object> xsPlaceholderReplacementValues = StepsUtil.getXsPlaceholderReplacementValues(context);
        return (String) xsPlaceholderReplacementValues.get(SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER);
    }

    private Map<String, Set<Integer>> getUsedApplicationPorts(DelegateExecution context) {
        List<Module> modulesToDeploy = getModulesToDeploy(context);
        if (CollectionUtils.isEmpty(modulesToDeploy)) {
            return Collections.emptyMap();
        }
        ApplicationCloudModelBuilder applicationCloudModelBuilder = getApplicationCloudModelBuilder(context);

        return modulesToDeploy.stream()
            .filter(module -> moduleToDeployHelper.isApplication(module))
            .collect(Collectors.toMap(module -> module.getName(), module -> getOccupiedPorts(applicationCloudModelBuilder, module)));
    }

    private Set<Integer> getOccupiedPorts(ApplicationCloudModelBuilder applicationCloudModelBuilder, Module module) {
        List<String> applicationUris = applicationCloudModelBuilder.getApplicationUris(module);
        return OccupiedPortsDetector.detectOccupiedPorts(applicationUris);
    }

    protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getApplicationCloudModelBuilder(context, getStepLogger());
    }

    protected List<Module> getModulesToDeploy(DelegateExecution context) {
        return StepsUtil.getModulesToDeploy(context);
    }
}
