package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServiceKeysCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;

public class BuildCloudDeployModelStep extends SyncFlowableStep {

    protected SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.BUILDING_CLOUD_MODEL);
            DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(execution.getContext());

            // Get module sets:
            DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
            List<DeployedMtaModule> deployedModules = (deployedMta != null) ? deployedMta.getModules() : Collections.emptyList();
            Set<String> mtaArchiveModules = StepsUtil.getMtaArchiveModules(execution.getContext());
            getStepLogger().debug(Messages.MTA_ARCHIVE_MODULES, mtaArchiveModules);
            Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedModules);
            getStepLogger().debug(Messages.DEPLOYED_MODULES, deployedModuleNames);
            Set<String> mtaModules = StepsUtil.getMtaModules(execution.getContext());
            getStepLogger().debug(Messages.MTA_MODULES, mtaModules);

            StepsUtil.setNewMtaVersion(execution.getContext(), deploymentDescriptor.getVersion());

            // Build a map of service keys and save them in the context:
            Map<String, List<ServiceKey>> serviceKeys = getServiceKeysCloudModelBuilder(execution.getContext()).build();
            getStepLogger().debug(Messages.SERVICE_KEYS_TO_CREATE, secureSerializer.toJson(serviceKeys));

            StepsUtil.setServiceKeysToCreate(execution.getContext(), serviceKeys);

            // Build a list of applications for deployment and save them in the context:
            ApplicationsCloudModelBuilder applicationsCloudModelBuilder = getApplicationsCloudModelBuilder(execution.getContext());
            List<CloudApplicationExtended> apps = applicationsCloudModelBuilder.build(mtaArchiveModules, mtaModules, deployedModuleNames);
            getStepLogger().debug(Messages.APPS_TO_DEPLOY, secureSerializer.toJson(apps));
            StepsUtil.setAppsToDeploy(execution.getContext(), apps);
            StepsUtil.setDeploymentMode(execution.getContext(), applicationsCloudModelBuilder.getDeploymentMode());
            StepsUtil.setServiceKeysCredentialsToInject(execution.getContext(), new HashMap<>());
            StepsUtil.setUseIdleUris(execution.getContext(), false);

            // Build a list of custom domains and save them in the context:
            List<String> customDomainsFromApps = StepsUtil.getDomainsFromApps(execution.getContext(), apps);
            StepsUtil.setCustomDomains(execution.getContext(), customDomainsFromApps);
            getStepLogger().debug(Messages.CUSTOM_DOMAINS, customDomainsFromApps);

            List<CloudServiceExtended> allServices = getServicesCloudModelBuilder(execution.getContext()).build();

            // Build a list of services for binding and save them in the context:
            StepsUtil.setServicesToBind(execution.getContext(), allServices);

            // Build a list of services for creation and save them in the context:
            List<CloudServiceExtended> servicesToCreate = allServices.stream()
                .filter(CloudServiceExtended::isManaged)
                .collect(Collectors.toList());
            getStepLogger().debug(Messages.SERVICES_TO_CREATE, secureSerializer.toJson(servicesToCreate));
            StepsUtil.setServicesToCreate(execution.getContext(), servicesToCreate);

            // Needed by CreateOrUpdateServicesStep, as it is used as an iteration variable:
            execution.getContext()
                .setVariable(Constants.VAR_SERVICES_TO_CREATE_COUNT, 0);

            getStepLogger().debug(Messages.CLOUD_MODEL_BUILT);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_BUILDING_CLOUD_MODEL);
            throw e;
        }
    }

    protected ServiceKeysCloudModelBuilder getServiceKeysCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getServiceKeysCloudModelBuilder(context);
    }

    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getApplicationsCloudModelBuilder(context, getStepLogger());
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DelegateExecution context) {
        return StepsUtil.getServicesCloudModelBuilder(context, getStepLogger());
    }

}
