package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.detect.ApplicationMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("checkForCreationConflictsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckForCreationConflictsStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
        List<CloudApplication> deployedApps = StepsUtil.getDeployedApps(execution.getContext());
        try {
            getStepLogger().info(Messages.VALIDATING_SERVICES);
            CloudFoundryOperations client = execution.getCloudFoundryClient();
            validateServicesToCreate(client, execution.getContext(), deployedMta, deployedApps);
            getStepLogger().debug(Messages.SERVICES_VALIDATED);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_VALIDATING_SERVICES);
            throw e;
        } catch (CloudFoundryException cfe) {
            CloudControllerException e = new CloudControllerException(cfe);
            getStepLogger().error(e, Messages.ERROR_VALIDATING_SERVICES);
            throw e;
        }

        try {
            getStepLogger().info(Messages.VALIDATING_APPLICATIONS);
            validateApplicationsToDeploy(execution.getContext(), deployedMta, deployedApps);
            getStepLogger().debug(Messages.APPLICATIONS_VALIDATED);
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_VALIDATING_APPLICATIONS);
            throw e;
        } catch (CloudFoundryException cfe) {
            CloudControllerException e = new CloudControllerException(cfe);
            getStepLogger().error(e, Messages.ERROR_VALIDATING_APPLICATIONS);
            throw e;
        }

        return StepPhase.DONE;
    }

    private void validateServicesToCreate(CloudFoundryOperations client, DelegateExecution context, DeployedMta deployedMta,
        List<CloudApplication> deployedApps) {
        List<CloudServiceExtended> servicesToCreate = StepsUtil.getServicesToCreate(context);
        Map<String, CloudService> existingServicesMap = createExistingServicesMap(client.getServices());
        Set<String> servicesInDeployedMta = deployedMta != null ? deployedMta.getServices() : Collections.emptySet();
        for (CloudServiceExtended service : servicesToCreate) {
            if (existingServicesMap.containsKey(service.getName())) {
                validateExistingServiceAssociation(service, client, deployedApps, servicesInDeployedMta);
            }
        }
    }

    private void validateExistingServiceAssociation(CloudServiceExtended serviceToCreate, CloudFoundryOperations client,
        List<CloudApplication> deployedApps, Set<String> servicesInDeployedMta) {

        getStepLogger().debug(Messages.VALIDATING_EXISTING_SERVICE_ASSOCIATION, serviceToCreate.getName());
        if (servicesInDeployedMta.contains(serviceToCreate.getName())) {
            return;
        }

        List<CloudServiceBinding> bindings = getServiceBindings(client, serviceToCreate);
        if (bindings.size() == 0) {
            getStepLogger().warn(Messages.SERVICE_DOESNT_HAVE_BOUND_COMPONENTS, serviceToCreate.getName());
            return;
        }
        Set<String> namesOfBoundStandaloneApplications = new LinkedHashSet<>();
        Set<String> idsOfMtasThatOwnTheService = new LinkedHashSet<>();
        for (CloudServiceBinding binding : bindings) {
            CloudApplication boundApplication = getBoundApplication(binding, deployedApps);
            ApplicationMtaMetadata boundMtaMetadata = ApplicationMtaMetadataParser.parseAppMetadata(boundApplication);
            if (boundMtaMetadata == null) {
                namesOfBoundStandaloneApplications.add(boundApplication.getName());
            } else if (mtaIsOwnerOfService(boundMtaMetadata, serviceToCreate)) {
                idsOfMtasThatOwnTheService.add(boundMtaMetadata.getMtaMetadata()
                    .getId());
            }
        }
        if (!namesOfBoundStandaloneApplications.isEmpty()) {
            getStepLogger().warn(Messages.SERVICE_ASSOCIATED_WITH_OTHER_APPS, serviceToCreate.getName(),
                String.join(", ", namesOfBoundStandaloneApplications));
        }
        if (!idsOfMtasThatOwnTheService.isEmpty()) {
            throw new SLException(Messages.SERVICE_ASSOCIATED_WITH_OTHER_MTAS, serviceToCreate.getName(),
                String.join(", ", idsOfMtasThatOwnTheService));
        }
    }

    private boolean mtaIsOwnerOfService(ApplicationMtaMetadata mtaMetadata, CloudServiceExtended service) {
        return mtaMetadata.getServices()
            .contains(service.getName());
    }

    private List<CloudServiceBinding> getServiceBindings(CloudFoundryOperations client, CloudServiceExtended service) {
        CloudServiceInstance serviceInstance = client.getServiceInstance(service.getName());
        return serviceInstance.getBindings();
    }

    private CloudApplication getBoundApplication(CloudServiceBinding binding, List<CloudApplication> deployedApps) {
        return deployedApps.stream()
            .filter(app -> app.getMeta()
                .getGuid()
                .equals(binding.getAppGuid()))
            .findAny()
            .get();
    }

    private void validateApplicationsToDeploy(DelegateExecution context, DeployedMta deployedMta, List<CloudApplication> deployedApps) {

        List<CloudApplicationExtended> applicationsToDeploy = StepsUtil.getAppsToDeploy(context);
        Map<String, CloudApplication> existingApplicationsMap = createExistingApplicationsMap(deployedApps);
        List<DeployedMtaModule> deployedMtaModules = deployedMta != null ? deployedMta.getModules() : Collections.emptyList();
        Set<String> applicationsInDeployedMta = getApplicationsInDeployedMta(deployedMtaModules);

        for (CloudApplicationExtended application : applicationsToDeploy) {
            if (existingApplicationsMap.containsKey(application.getName())) {
                validateApplicationToDeploy(applicationsInDeployedMta, application, existingApplicationsMap);
            }
        }
    }

    private void validateApplicationToDeploy(Set<String> applicationsInDeployedMta, CloudApplicationExtended applicationToDeploy,
        Map<String, CloudApplication> existingApplicationsMap) {
        getStepLogger().debug(Messages.VALIDATING_EXISTING_APPLICATION_ASSOCIATION, applicationToDeploy.getName());
        if (applicationsInDeployedMta.contains(applicationToDeploy.getName())) {
            return;
        }
        Optional<DeployedMta> owningMta = detectOwningMta(applicationToDeploy, existingApplicationsMap.values());
        if (!owningMta.isPresent()) {
            getStepLogger().warn(Messages.APPLICATION_EXISTS_AS_STANDALONE, applicationToDeploy.getName());
            return;
        }
        String owningMtaId = owningMta.get()
            .getMetadata()
            .getId();
        throw new SLException(Messages.APPLICATION_ASSOCIATED_WITH_ANOTHER_MTA, applicationToDeploy.getName(), owningMtaId);
    }

    private Optional<DeployedMta> detectOwningMta(CloudApplicationExtended application, Collection<CloudApplication> deployedApps) {
        List<DeployedMta> deployedMtas = getDeployedMtas(deployedApps);
        return deployedMtas.stream()
            .filter(mta -> deployedMtaContainsApplication(mta, application))
            .findAny();
    }

    private List<DeployedMta> getDeployedMtas(Collection<CloudApplication> deployedApps) {
        return new DeployedComponentsDetector().detectAllDeployedComponents(deployedApps)
            .getMtas();
    }

    private boolean deployedMtaContainsApplication(DeployedMta deployedMta, CloudApplication existingApp) {
        String appName = existingApp.getName();
        return deployedMta == null ? false : deployedMta.getModules()
            .stream()
            .anyMatch(module -> module.getAppName()
                .equals(appName));
    }

    private Map<String, CloudApplication> createExistingApplicationsMap(List<CloudApplication> existingApps) {
        Map<String, CloudApplication> applicationsMap = new HashMap<>(existingApps.size());
        existingApps.forEach(app -> applicationsMap.put(app.getName(), app));
        return applicationsMap;
    }

    private Map<String, CloudService> createExistingServicesMap(List<CloudService> existingServices) {
        Map<String, CloudService> servicesMap = new HashMap<>(existingServices.size());
        existingServices.forEach(service -> servicesMap.put(service.getName(), service));
        return servicesMap;
    }

    private Set<String> getApplicationsInDeployedMta(List<DeployedMtaModule> modules) {
        return modules.stream()
            .map(module -> module.getAppName())
            .collect(Collectors.toSet());
    }

}
