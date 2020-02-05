package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil.hasEnvMtaMetadata;
import static com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil.hasMtaMetadata;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.v3.Metadata;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataLabels;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaService;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Named("checkForCreationConflictsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckForCreationConflictsStep extends SyncFlowableStep {

    @Inject
    private MtaMetadataParser mtaMetadataParser;

    @Inject
    private EnvMtaMetadataParser envMtaMetadataParser;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws CloudOperationException, SLException {
        DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
        try {
            getStepLogger().debug(Messages.VALIDATING_SERVICES);
            CloudControllerClient client = execution.getControllerClient();
            validateServicesToCreate(client, execution.getContext(), deployedMta);
            getStepLogger().debug(Messages.SERVICES_VALIDATED);
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_VALIDATING_SERVICES);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_VALIDATING_SERVICES);
            throw e;
        }

        try {
            getStepLogger().debug(Messages.VALIDATING_APPLICATIONS);
            validateApplicationsToDeploy(execution, deployedMta);
            getStepLogger().debug(Messages.APPLICATIONS_VALIDATED);
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_VALIDATING_APPLICATIONS);
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_VALIDATING_APPLICATIONS);
            throw e;
        }

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_VALIDATING_APPLICATIONS;
    }

    private void validateServicesToCreate(CloudControllerClient client, DelegateExecution context, DeployedMta deployedMta) {
        List<CloudServiceExtended> servicesToCreate = StepsUtil.getServicesToCreate(context);
        List<DeployedMtaService> servicesInDeployedMta = deployedMta != null ? deployedMta.getServices() : Collections.emptyList();
        for (CloudServiceExtended service : servicesToCreate) {
            CloudServiceInstance existingServiceInstance = client.getServiceInstance(service.getName(), false);
            if (existingServiceInstance != null) {
                validateExistingServiceAssociation(service, existingServiceInstance, servicesInDeployedMta, client);
            }
        }
    }

    private void validateExistingServiceAssociation(CloudServiceExtended serviceToCreate, CloudServiceInstance existingServiceInstance,
                                                    List<DeployedMtaService> servicesInDeployedMta, CloudControllerClient client) {
        Set<String> serviceNamesInDeployedMta = servicesInDeployedMta.stream()
                                                                     .map(DeployedMtaService::getName)
                                                                     .collect(Collectors.toSet());
        getStepLogger().debug(Messages.VALIDATING_EXISTING_SERVICE_ASSOCIATION, serviceToCreate.getName());
        if (serviceNamesInDeployedMta.contains(serviceToCreate.getName())) {
            return;
        }

        List<CloudServiceBinding> bindings = existingServiceInstance.getBindings();
        if (bindings.isEmpty()) {
            getStepLogger().warn(Messages.SERVICE_DOESNT_HAVE_BOUND_COMPONENTS, serviceToCreate.getName());
            return;
        }
        Set<String> namesOfBoundStandaloneApplications = new LinkedHashSet<>();
        Set<String> idsOfMtasThatOwnTheService = new LinkedHashSet<>();
        for (CloudServiceBinding binding : bindings) {
            CloudApplication boundApplication = client.getApplication(binding.getApplicationGuid());
            if (boundApplication == null) {
                throw new IllegalStateException(MessageFormat.format(Messages.COULD_NOT_FIND_APPLICATION_WITH_GUID_0,
                                                                     binding.getApplicationGuid()));
            }

            MtaMetadata mtaMetadata = getMtaMetadata(boundApplication);
            if (mtaMetadata == null) {
                namesOfBoundStandaloneApplications.add(boundApplication.getName());
                continue;
            }
            if (isServicePartOfMta(getDeployedMtaApplication(boundApplication), serviceToCreate)) {
                idsOfMtasThatOwnTheService.add(mtaMetadata.getId());
            }
        }
        if (!namesOfBoundStandaloneApplications.isEmpty()) {
            getStepLogger().warn(Messages.SERVICE_ASSOCIATED_WITH_OTHER_APPS, serviceToCreate.getName(),
                                 String.join(", ", namesOfBoundStandaloneApplications));
        }
        if (!idsOfMtasThatOwnTheService.isEmpty()) {
            throw new SLException(Messages.SERVICE_ASSOCIATED_WITH_OTHER_MTAS,
                                  serviceToCreate.getName(),
                                  String.join(", ", idsOfMtasThatOwnTheService));
        }
    }

    private MtaMetadata getMtaMetadata(CloudApplication app) {
        if (hasMtaMetadata(app)) {
            return mtaMetadataParser.parseMtaMetadata(app);
        } else if (hasEnvMtaMetadata(app)) {
            return envMtaMetadataParser.parseMtaMetadata(app);
        }
        return null;
    }

    private DeployedMtaApplication getDeployedMtaApplication(CloudApplication app) {
        if (hasMtaMetadata(app)) {
            return mtaMetadataParser.parseDeployedMtaApplication(app);
        }
        return envMtaMetadataParser.parseDeployedMtaApplication(app);
    }

    private boolean isServicePartOfMta(DeployedMtaApplication deployedMtaApplication, CloudServiceExtended service) {
        return deployedMtaApplication.getBoundMtaServices()
                                     .stream()
                                     .anyMatch(boundMtaService -> boundMtaService.equals(service.getName()));
    }

    private void validateApplicationsToDeploy(ExecutionWrapper execution, DeployedMta deployedMta) {
        List<String> appNames = StepsUtil.getAppsToDeploy(execution.getContext());
        List<DeployedMtaApplication> applications = deployedMta != null ? deployedMta.getApplications() : Collections.emptyList();
        Set<String> applicationsInDeployedMta = getApplicationNames(applications);
        for (String appName : appNames) {
            validateApplicationToDeploy(applicationsInDeployedMta, appName, execution.getControllerClient());
        }
    }

    private void validateApplicationToDeploy(Set<String> applicationsInDeployedMta, String appName, CloudControllerClient client) {
        getStepLogger().debug(Messages.VALIDATING_EXISTING_APPLICATION_ASSOCIATION, appName);
        if (applicationsInDeployedMta.contains(appName)) {
            return;
        }
        CloudApplication application = client.getApplication(appName, false);
        if (application == null) {
            return;
        }
        String owningMtaId = detectOwningMtaId(application, client);
        if (StringUtils.isBlank(owningMtaId)) {
            getStepLogger().warn(Messages.APPLICATION_EXISTS_AS_STANDALONE, appName);
            return;
        }
        throw new SLException(Messages.APPLICATION_ASSOCIATED_WITH_ANOTHER_MTA, appName, owningMtaId);
    }

    private String detectOwningMtaId(CloudApplication application, CloudControllerClient client) {
        Metadata metadata = application.getV3Metadata();
        if (metadata != null) {
            return metadata.getLabels()
                           .get(MtaMetadataLabels.MTA_ID);
        }

        if (hasEnvMtaMetadata(application)) {
            return envMtaMetadataParser.parseMtaMetadata(application)
                                       .getId();
        }
        return null;
    }

    private Map<String, CloudApplication> createExistingApplicationsMap(List<CloudApplication> existingApps) {
        Map<String, CloudApplication> applicationsMap = new HashMap<>(existingApps.size());
        existingApps.forEach(app -> applicationsMap.put(app.getName(), app));
        return applicationsMap;
    }

    private Set<String> getApplicationNames(List<DeployedMtaApplication> deployedMtaApplications) {
        return deployedMtaApplications.stream()
                                      .map(CloudApplication::getName)
                                      .collect(Collectors.toSet());
    }

}
