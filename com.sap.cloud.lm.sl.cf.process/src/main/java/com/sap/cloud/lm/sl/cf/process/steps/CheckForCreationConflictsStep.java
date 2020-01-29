package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.detect.ApplicationMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Named("checkForCreationConflictsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckForCreationConflictsStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws CloudOperationException, SLException {
        DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
        List<CloudApplication> deployedApps = StepsUtil.getDeployedApps(execution.getContext());
        try {
            getStepLogger().debug(Messages.VALIDATING_SERVICES);
            CloudControllerClient client = execution.getControllerClient();
            validateServicesToCreate(client, execution.getContext(), deployedMta, deployedApps);
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
            validateApplicationsToDeploy(execution.getContext(), deployedMta, deployedApps);
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

    private void validateServicesToCreate(CloudControllerClient client, DelegateExecution context, DeployedMta deployedMta,
                                          List<CloudApplication> deployedApps) {
        List<CloudServiceExtended> servicesToCreate = StepsUtil.getServicesToCreate(context);
        Set<String> servicesInDeployedMta = deployedMta != null ? deployedMta.getServices() : Collections.emptySet();
        for (CloudServiceExtended service : servicesToCreate) {
            CloudServiceInstance existingServiceInstance = client.getServiceInstance(service.getName(), false);
            if (existingServiceInstance != null) {
                validateExistingServiceAssociation(service, existingServiceInstance, deployedApps, servicesInDeployedMta);
            }
        }
    }

    private void validateExistingServiceAssociation(CloudServiceExtended serviceToCreate, CloudServiceInstance existingServiceInstance,
                                                    List<CloudApplication> deployedApps, Set<String> servicesInDeployedMta) {

        getStepLogger().debug(Messages.VALIDATING_EXISTING_SERVICE_ASSOCIATION, serviceToCreate.getName());
        if (servicesInDeployedMta.contains(serviceToCreate.getName())) {
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
            CloudApplication boundApplication = StepsUtil.getBoundApplication(deployedApps, binding.getApplicationGuid());
            if (boundApplication == null) {
                throw new IllegalStateException(MessageFormat.format(Messages.COULD_NOT_FIND_APPLICATION_WITH_GUID_0,
                                                                     binding.getApplicationGuid()));
            }
            ApplicationMtaMetadata boundMtaMetadata = ApplicationMtaMetadataParser.parseAppMetadata(boundApplication);
            if (boundMtaMetadata == null) {
                namesOfBoundStandaloneApplications.add(boundApplication.getName());
                continue;
            }
            if (isServicePartOfMta(boundMtaMetadata, serviceToCreate)) {
                idsOfMtasThatOwnTheService.add(boundMtaMetadata.getMtaMetadata()
                                                               .getId());
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

    private boolean isServicePartOfMta(ApplicationMtaMetadata mtaMetadata, CloudServiceExtended service) {
        return mtaMetadata.getServices()
                          .contains(service.getName());
    }

    private void validateApplicationsToDeploy(DelegateExecution context, DeployedMta deployedMta, List<CloudApplication> deployedApps) {
        List<String> appNames = StepsUtil.getAppsToDeploy(context);
        Map<String, CloudApplication> existingApplicationsMap = createExistingApplicationsMap(deployedApps);
        List<DeployedMtaApplication> deployedMtaApplications = deployedMta != null ? deployedMta.getApplications()
            : Collections.emptyList();
        Set<String> applicationsInDeployedMta = getApplicationsInDeployedMta(deployedMtaApplications);

        for (String appName : appNames) {
            if (existingApplicationsMap.containsKey(appName)) {
                validateApplicationToDeploy(applicationsInDeployedMta, appName, existingApplicationsMap);
            }
        }
    }

    private void validateApplicationToDeploy(Set<String> applicationsInDeployedMta, String appName,
                                             Map<String, CloudApplication> existingApplicationsMap) {
        getStepLogger().debug(Messages.VALIDATING_EXISTING_APPLICATION_ASSOCIATION, appName);
        if (applicationsInDeployedMta.contains(appName)) {
            return;
        }
        Optional<DeployedMta> owningMta = detectOwningMta(appName, existingApplicationsMap.values());
        if (!owningMta.isPresent()) {
            getStepLogger().warn(Messages.APPLICATION_EXISTS_AS_STANDALONE, appName);
            return;
        }
        String owningMtaId = owningMta.get()
                                      .getMetadata()
                                      .getId();
        throw new SLException(Messages.APPLICATION_ASSOCIATED_WITH_ANOTHER_MTA, appName, owningMtaId);
    }

    private Optional<DeployedMta> detectOwningMta(String appName, Collection<CloudApplication> deployedApps) {
        List<DeployedMta> deployedMtas = getDeployedMtas(deployedApps);
        return deployedMtas.stream()
                           .filter(mta -> deployedMtaContainsApplication(mta, appName))
                           .findAny();
    }

    private List<DeployedMta> getDeployedMtas(Collection<CloudApplication> deployedApps) {
        return new DeployedComponentsDetector().detectAllDeployedComponents(deployedApps)
                                               .getMtas();
    }

    private boolean deployedMtaContainsApplication(DeployedMta deployedMta, String appName) {
        return deployedMta != null && deployedMta.getApplications()
                                                 .stream()
                                                 .anyMatch(deployedApplication -> deployedApplication.getAppName()
                                                                                                     .equals(appName));
    }

    private Map<String, CloudApplication> createExistingApplicationsMap(List<CloudApplication> existingApps) {
        Map<String, CloudApplication> applicationsMap = new HashMap<>(existingApps.size());
        existingApps.forEach(app -> applicationsMap.put(app.getName(), app));
        return applicationsMap;
    }

    private Set<String> getApplicationsInDeployedMta(List<DeployedMtaApplication> deployedMtaApplications) {
        return deployedMtaApplications.stream()
                                      .map(DeployedMtaApplication::getAppName)
                                      .collect(Collectors.toSet());
    }

}
