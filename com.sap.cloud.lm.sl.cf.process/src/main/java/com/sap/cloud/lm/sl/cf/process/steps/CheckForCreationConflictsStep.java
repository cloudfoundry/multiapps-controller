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

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.detect.ApplicationMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteria;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteriaBuilder;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("checkForCreationConflictsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CheckForCreationConflictsStep extends SyncFlowableStep {

    @Autowired
    private DeployedComponentsDetector deployedComponentsDetector;

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
            validateApplicationsToDeploy(execution, deployedMta, deployedApps);
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

    private void validateServicesToCreate(CloudControllerClient client, DelegateExecution context, DeployedMta deployedMta,
        List<CloudApplication> deployedApps) {
        List<CloudServiceExtended> servicesToCreate = StepsUtil.getServicesToCreate(context);
        Map<String, CloudService> existingServicesMap = createExistingServicesMap(client.getServices());
        Set<DeployedMtaResource> servicesInDeployedMta = deployedMta != null ? deployedMta.getServices() : Collections.emptySet();
        servicesInDeployedMta.stream()
                             .collect(Collectors.toMap(v -> v.getServiceName(), v -> v, (v1, v2) -> v1));
        for (CloudServiceExtended service : servicesToCreate) {
            if (existingServicesMap.containsKey(service.getName())) {
                validateExistingServiceAssociation(service, client, deployedApps, servicesInDeployedMta);
            }
        }
    }

    private void validateExistingServiceAssociation(CloudServiceExtended serviceToCreate, CloudControllerClient client,
        List<CloudApplication> deployedApps, Set<DeployedMtaResource> servicesInDeployedMta) {
        Set<String> serviceNamesInDeployedMta = servicesInDeployedMta.stream()
                                                                     .map(s -> s.getServiceName())
                                                                     .collect(Collectors.toSet());
        getStepLogger().debug(Messages.VALIDATING_EXISTING_SERVICE_ASSOCIATION, serviceToCreate.getName());
        if (serviceNamesInDeployedMta.contains(serviceToCreate.getName())) {
            return;
        }

        List<CloudServiceBinding> bindings = getServiceBindings(client, serviceToCreate);
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
            
            //TODO
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
                          .stream()
                          .filter(s -> s.getServiceName()
                                        .equals(service.getName()))
                          .findAny()
                          .isPresent();
    }

    private List<CloudServiceBinding> getServiceBindings(CloudControllerClient client, CloudServiceExtended service) {
        CloudServiceInstance serviceInstance = client.getServiceInstance(service.getName());
        return serviceInstance.getBindings();
    }

    private void validateApplicationsToDeploy(ExecutionWrapper execution, DeployedMta deployedMta, List<CloudApplication> deployedApps) {
        List<String> appNames = StepsUtil.getAppsToDeploy(execution.getContext());
        Map<String, CloudApplication> existingApplicationsMap = createExistingApplicationsMap(deployedApps);
        List<DeployedMtaModule> deployedMtaModules = deployedMta != null ? deployedMta.getModules() : Collections.emptyList();
        Set<String> applicationsInDeployedMta = getApplicationsInDeployedMta(deployedMtaModules);

        for (String appName : appNames) {
            if (existingApplicationsMap.containsKey(appName)) {
                validateApplicationToDeploy(applicationsInDeployedMta, appName, existingApplicationsMap, execution.getControllerClient());
            }
        }
    }

    private void validateApplicationToDeploy(Set<String> applicationsInDeployedMta, String appName,
        Map<String, CloudApplication> existingApplicationsMap, CloudControllerClient cloudControllerClient) {
        getStepLogger().debug(Messages.VALIDATING_EXISTING_APPLICATION_ASSOCIATION, appName);
        if (applicationsInDeployedMta.contains(appName)) {
            return;
        }
        Optional<DeployedMta> owningMta = detectOwningMta(appName, existingApplicationsMap.values(), cloudControllerClient);
        if (!owningMta.isPresent()) {
            getStepLogger().warn(Messages.APPLICATION_EXISTS_AS_STANDALONE, appName);
            return;
        }
        String owningMtaId = owningMta.get()
                                      .getMetadata()
                                      .getId();
        throw new SLException(Messages.APPLICATION_ASSOCIATED_WITH_ANOTHER_MTA, appName, owningMtaId);
    }

    private Optional<DeployedMta> detectOwningMta(String appName, Collection<CloudApplication> deployedApps,
        CloudControllerClient cloudControllerClient) {
        // TODO use the embeded metadata in CloudApplication which currently does not exist
        //It should be included when we start using the pure cf java client for V3 api (and when the metadata is included in it)
        MtaMetadataCriteria criteria = new MtaMetadataCriteriaBuilder().label(MtaMetadataCriteriaBuilder.LABEL_APP_NAME)
                                                                       .haveValue(appName)
                                                                       .build();
        return deployedComponentsDetector.getDeployedMtaByCriteria(criteria, null);
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
                      .map(DeployedMtaModule::getAppName)
                      .collect(Collectors.toSet());
    }

}
