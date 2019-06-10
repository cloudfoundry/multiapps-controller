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

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.v3.Metadata;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.detect.ApplicationMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.ApplicationMetadataFieldExtractor;
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
    private ApplicationMetadataFieldExtractor applicationMetadataMapper;
    
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

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_VALIDATING_APPLICATIONS;
    }

    private void validateServicesToCreate(CloudControllerClient client, DelegateExecution context, DeployedMta deployedMta,
                                          List<CloudApplication> deployedApps) {
        List<CloudServiceExtended> servicesToCreate = StepsUtil.getServicesToCreate(context);
        Map<String, CloudService> existingServicesMap = createExistingServicesMap(client.getServices());
        List<DeployedMtaResource> servicesInDeployedMta = deployedMta != null ? deployedMta.getServices() : Collections.emptyList();
        for (CloudServiceExtended service : servicesToCreate) {
            if (existingServicesMap.containsKey(service.getName())) {
                validateExistingServiceAssociation(service, client, deployedApps, servicesInDeployedMta);
            }
        }
    }

    private void validateExistingServiceAssociation(CloudServiceExtended serviceToCreate, CloudControllerClient client,
        List<CloudApplication> deployedApps, List<DeployedMtaResource> servicesInDeployedMta) {
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

            ApplicationMtaMetadata boundMtaMetadata = getApplicationMtaMetadata(boundApplication);
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

    private ApplicationMtaMetadata getApplicationMtaMetadata(CloudApplication app) {
        if(app.getMetadata() == null) {
            return ApplicationMtaMetadataParser.parseAppMetadata(app);
        } else {
            return applicationMetadataMapper.extractMetadata(app);
        }
    }

    private boolean isServicePartOfMta(ApplicationMtaMetadata mtaMetadata, CloudServiceExtended service) {
        return mtaMetadata.getDeployedMtaModule().getServices()
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
        String owningMtaId = detectOwningMtaId(appName, existingApplicationsMap.values(), cloudControllerClient);
        if (StringUtils.isBlank(owningMtaId)) {
            getStepLogger().warn(Messages.APPLICATION_EXISTS_AS_STANDALONE, appName);
            return;
        }
        throw new SLException(Messages.APPLICATION_ASSOCIATED_WITH_ANOTHER_MTA, appName, owningMtaId);
    }

    private String detectOwningMtaId(String appName, Collection<CloudApplication> deployedApps,
        CloudControllerClient cloudControllerClient) {
        // TODO use the embeded metadata in CloudApplication which currently does not exist
        //It should be included when we start using the pure cf java client for V3 api (and when the metadata is included in it)
        Optional<CloudApplication> selectedApp = deployedApps.stream().filter(a -> a.getName().equalsIgnoreCase(appName)).findFirst();
        if(!selectedApp.isPresent()) {
            return null;
        }
        CloudApplication app = selectedApp.get();
        ApplicationMtaMetadata parsedAppMetadata = ApplicationMtaMetadataParser.parseAppMetadata(app);
        if(parsedAppMetadata != null) {
            return parsedAppMetadata.getMtaMetadata().getId();
        }
        Metadata metadata = app.getV3Metadata();
        if(metadata == null) {
            return null;
        }
        return applicationMetadataMapper.getMtaId(metadata);
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
