package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.detect.ApplicationMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("checkForCreationConflictsStep")
public class CheckForCreationConflictsStep extends AbstractXS2ProcessStep {
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckForCreationConflictsStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("checkForCreationConflictsTask", "Check For Creation Conflicts", "Check For Creation Conflicts");
    }

    protected Function<DelegateExecution, CloudFoundryOperations> clientSupplier = (context) -> getCloudFoundryClient(context, LOGGER);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception {
        logActivitiTask(context, LOGGER);

        DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
        List<CloudApplication> deployedApps = StepsUtil.getDeployedApps(context);
        try {
            info(context, Messages.VALIDATING_SERVICES, LOGGER);
            validateServicesToCreate(context, deployedMta, deployedApps);
            debug(context, Messages.SERVICES_VALIDATED, LOGGER);
        } catch (SLException e) {
            error(context, Messages.ERROR_VALIDATING_SERVICES, e, LOGGER);
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_VALIDATING_SERVICES, e, LOGGER);
            throw e;
        }

        try {
            info(context, Messages.VALIDATING_APPLICATIONS, LOGGER);
            validateApplicationsToDeploy(context, deployedMta, deployedApps);
            debug(context, Messages.APPLICATIONS_VALIDATED, LOGGER);
        } catch (SLException e) {
            error(context, Messages.ERROR_VALIDATING_APPLICATIONS, e, LOGGER);
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            error(context, Messages.ERROR_VALIDATING_APPLICATIONS, e, LOGGER);
            throw e;
        }

        return ExecutionStatus.SUCCESS;
    }

    private void validateServicesToCreate(DelegateExecution context, DeployedMta deployedMta, List<CloudApplication> deployedApps) {
        CloudFoundryOperations client = clientSupplier.apply(context);
        List<CloudServiceExtended> servicesToCreate = StepsUtil.getServicesToCreate(context);
        Map<String, CloudService> existingServicesMap = createExistingServicesMap(client.getServices());
        Set<String> servicesInDeployedMta = deployedMta != null ? deployedMta.getServices() : Collections.emptySet();
        for (CloudServiceExtended service : servicesToCreate) {
            if (existingServicesMap.containsKey(service.getName())) {
                validateExistingServiceAssociation(context, service, client, deployedApps, servicesInDeployedMta);
            }
        }
    }

    private void validateExistingServiceAssociation(DelegateExecution context, CloudServiceExtended serviceToCreate,
        CloudFoundryOperations client, List<CloudApplication> deployedApps, Set<String> servicesInDeployedMta) {

        debug(context, format(Messages.VALIDATING_EXISTING_SERVICE_ASSOCIATION, serviceToCreate.getName()), LOGGER);
        if (servicesInDeployedMta.contains(serviceToCreate.getName())) {
            return;
        }

        List<CloudServiceBinding> bindings = getServiceBindings(client, serviceToCreate);
        if (bindings.size() == 0) {
            warn(context, format(Messages.SERVICE_DOESNT_HAVE_BOUND_COMPONENTS, serviceToCreate.getName()), LOGGER);
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
                idsOfMtasThatOwnTheService.add(boundMtaMetadata.getMtaMetadata().getId());
            }
        }
        if (!namesOfBoundStandaloneApplications.isEmpty()) {
            throw new SLException(Messages.SERVICE_ASSOCIATED_WITH_OTHER_APPS, serviceToCreate.getName(),
                String.join(", ", namesOfBoundStandaloneApplications));
        }
        if (!idsOfMtasThatOwnTheService.isEmpty()) {
            throw new SLException(Messages.SERVICE_ASSOCIATED_WITH_OTHER_MTAS, serviceToCreate.getName(),
                String.join(", ", idsOfMtasThatOwnTheService));
        }
    }

    private boolean mtaIsOwnerOfService(ApplicationMtaMetadata mtaMetadata, CloudServiceExtended service) {
        return mtaMetadata.getServices().contains(service.getName());
    }

    private List<CloudServiceBinding> getServiceBindings(CloudFoundryOperations client, CloudServiceExtended service) {
        CloudServiceInstance serviceInstance = client.getServiceInstance(service.getName());
        return serviceInstance.getBindings();
    }

    private CloudApplication getBoundApplication(CloudServiceBinding binding, List<CloudApplication> deployedApps) {
        return deployedApps.stream().filter(app -> app.getMeta().getGuid().equals(binding.getAppGuid())).findAny().get();
    }

    private void validateApplicationsToDeploy(DelegateExecution context, DeployedMta deployedMta, List<CloudApplication> deployedApps) {

        List<CloudApplicationExtended> applicationsToDeploy = StepsUtil.getAppsToDeploy(context);
        Map<String, CloudApplication> existingApplicationsMap = createExistingApplicationsMap(deployedApps);
        List<DeployedMtaModule> deployedMtaModules = deployedMta != null ? deployedMta.getModules() : Collections.emptyList();
        Map<String, DeployedMtaModule> applicationsInDeployedMta = createApplicationsInDeployedMtaMap(deployedMtaModules);

        for (CloudApplicationExtended application : applicationsToDeploy) {
            if (existingApplicationsMap.containsKey(application.getName())) {
                validateExistingApplicationAssociation(context, application, deployedApps, applicationsInDeployedMta);
            }
        }
    }

    private void validateExistingApplicationAssociation(DelegateExecution context, CloudApplicationExtended existingApp,
        List<CloudApplication> deployedApps, Map<String, DeployedMtaModule> applicationsInDeployedMtaMap) {

        debug(context, format(Messages.VALIDATING_EXISTING_APPLICATION_ASSOCIATION, existingApp.getName()), LOGGER);
        if (!applicationsInDeployedMtaMap.containsKey(existingApp.getName())) {
            throw createApplicationValidationException(existingApp, deployedApps);
        }
    }

    private SLException createApplicationValidationException(CloudApplicationExtended existingApp, List<CloudApplication> deployedApps) {

        DeployedMta applicationOwner = null;
        try {
            applicationOwner = detectApplicationOwner(existingApp, deployedApps);
            String applicationOwnerId = applicationOwner.getMetadata().getId();
            return new SLException(format(Messages.APPLICATION_ASSOCIATED_WITH_ANOTHER_MTA, existingApp.getName(), applicationOwnerId));
        } catch (NoSuchElementException e) {
            return new SLException(format(Messages.APPLICATION_EXISTS_AS_STANDALONE, existingApp.getName()));
        }

    }

    private DeployedMta detectApplicationOwner(CloudApplicationExtended application, List<CloudApplication> deployedApps) {
        List<DeployedMta> deployedMtas = getDeployedMtas(deployedApps);
        return deployedMtas.stream().filter(mta -> deployedMtaContainsApplication(mta, application)).findAny().get();
    }

    private List<DeployedMta> getDeployedMtas(List<CloudApplication> deployedApps) {
        return new DeployedComponentsDetector().detectAllDeployedComponents(deployedApps).getMtas();
    }

    private boolean deployedMtaContainsApplication(DeployedMta deployedMta, CloudApplication existingApp) {
        String appName = existingApp.getName();
        return deployedMta == null ? false : deployedMta.getModules().stream().anyMatch(module -> module.getAppName().equals(appName));
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

    public Map<String, DeployedMtaModule> createApplicationsInDeployedMtaMap(List<DeployedMtaModule> modules) {
        Map<String, DeployedMtaModule> applicationsInDeployedMtaMap = new HashMap<>(modules.size());
        modules.forEach(module -> applicationsInDeployedMtaMap.put(module.getAppName(), module));
        return applicationsInDeployedMtaMap;
    }

}
