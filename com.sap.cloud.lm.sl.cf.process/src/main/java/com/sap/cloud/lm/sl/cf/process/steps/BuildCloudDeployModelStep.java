package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ServiceKey;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1_0.CloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.ProvidedDependency;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("buildCloudDeployModelStep")
public class BuildCloudDeployModelStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildCloudDeployModelStep.class);

    public static final String DEPLOY_ID_PREFIX = "deploy-";

    public static StepMetadata getMetadata() {
        return new StepMetadata("buildDeployModelTask", "Build Deploy Model", "Build Deploy Model");
    }

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private OngoingOperationDao ongoingOperationDao;

    protected Function<OngoingOperationDao, ProcessConflictPreventer> conflictPreventerSupplier = (dao) -> new ProcessConflictPreventer(
        ongoingOperationDao);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        try {
            info(context, Messages.BUILDING_CLOUD_MODEL, LOGGER);

            boolean allowInvalidEnvNames = ContextUtil.getVariable(context, Constants.PARAM_ALLOW_INVALID_ENV_NAMES, false);
            boolean useNamespaces = ContextUtil.getVariable(context, Constants.PARAM_USE_NAMESPACES, true);
            boolean useNamespacesForServices = ContextUtil.getVariable(context, Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, false);
            boolean portBasedRouting = ContextUtil.getVariable(context, Constants.VAR_PORT_BASED_ROUTING, false);

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);

            DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(context);
            conflictPreventerSupplier.apply(ongoingOperationDao).attemptToAcquireLock(deploymentDescriptor.getId(),
                StepsUtil.getSpaceId(context), context.getProcessInstanceId());

            DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
            SystemParameters systemParameters = StepsUtil.getSystemParameters(context);

            String deployId = DEPLOY_ID_PREFIX + context.getId();

            XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);

            CloudModelBuilder builder = getCloudModelBuilder(handlerFactory, deploymentDescriptor, systemParameters, portBasedRouting, true,
                useNamespaces, useNamespacesForServices, allowInvalidEnvNames, deployId, xsPlaceholderResolver);

            // Get module sets:
            List<DeployedMtaModule> deployedModules = (deployedMta != null) ? deployedMta.getModules() : Collections.emptyList();
            Set<String> mtaArchiveModules = StepsUtil.getMtaArchiveModules(context);
            debug(context, format(Messages.MTA_ARCHIVE_MODULES, mtaArchiveModules), LOGGER);
            Set<String> deployedModuleNames = CloudModelBuilderUtil.getDeployedModuleNames(deployedModules);
            debug(context, format(Messages.DEPLOYED_MODULES, deployedModuleNames), LOGGER);
            Set<String> mtaModules = StepsUtil.getMtaModules(context);
            debug(context, format(Messages.MTA_MODULES, mtaModules), LOGGER);

            // Build public provided dependencies list and save them in the context:
            List<ProvidedDependency> publicProvidedDependencies = getPublicProvidedDependencies(deploymentDescriptor.getModules1_0());
            StepsUtil.setDependenciesToPublish(context, publicProvidedDependencies);
            StepsUtil.setNewMtaVersion(context, deploymentDescriptor.getVersion());

            // Build a list of custom domains and save them in the context:
            List<String> customDomains = builder.getCustomDomains();
            debug(context, format(Messages.CUSTOM_DOMAINS, customDomains), LOGGER);
            StepsUtil.setCustomDomains(context, customDomains);

            // Build a map of service keys and save them in the context:
            Map<String, List<ServiceKey>> serviceKeys = builder.getServiceKeys();
            debug(context, format(Messages.SERVICE_KEYS_TO_CREATE, secureSerializer.toJson(serviceKeys)), LOGGER);

            StepsUtil.setServiceKeysToCreate(context, serviceKeys);

            // Build a list of applications for deployment and save them in the context:
            List<CloudApplicationExtended> apps = builder.getApplications(mtaArchiveModules, mtaModules, deployedModuleNames);
            debug(context, format(Messages.APPS_TO_DEPLOY, secureSerializer.toJson(apps)), LOGGER);
            StepsUtil.setAppsToDeploy(context, apps);

            // Build a list of services for creation and save them in the context:
            List<CloudServiceExtended> services = builder.getServices(mtaArchiveModules);
            debug(context, format(Messages.SERVICES_TO_CREATE, secureSerializer.toJson(services)), LOGGER);

            StepsUtil.setServicesToCreate(context, services);

            debug(context, Messages.CLOUD_MODEL_BUILT, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_BUILDING_CLOUD_MODEL, e, LOGGER);
            throw e;
        }
    }

    private List<ProvidedDependency> getPublicProvidedDependencies(List<Module> mtaModules) {
        List<ProvidedDependency> publicProvidedDependencies = new ArrayList<>();
        for (Module module : mtaModules) {
            List<ProvidedDependency> publicProvidedDependenciesForModule = module.getProvidedDependencies1_0().stream().filter(
                CloudModelBuilderUtil::isPublic).collect(Collectors.toList());
            publicProvidedDependencies.addAll(publicProvidedDependenciesForModule);
        }
        return publicProvidedDependencies;
    }

    protected CloudModelBuilder getCloudModelBuilder(HandlerFactory factory, DeploymentDescriptor descript,
        SystemParameters systemParameters, boolean portBasedRouting, boolean prettyPrinting, boolean useNamespaces,
        boolean useNamespacesForServices, boolean allowInvalidEnvNames, String deployId, XsPlaceholderResolver xsPlaceholderResolver) {
        return factory.getCloudModelBuilder(descript, systemParameters, portBasedRouting, prettyPrinting, useNamespaces,
            useNamespacesForServices, allowInvalidEnvNames, deployId, xsPlaceholderResolver);
    }

}
