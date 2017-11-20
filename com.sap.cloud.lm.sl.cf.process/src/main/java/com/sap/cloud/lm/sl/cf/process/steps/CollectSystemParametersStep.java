package com.sap.cloud.lm.sl.cf.process.steps;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.CredentialsGenerator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.SystemParametersBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.mta.model.VersionRule;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;

@Component("collectSystemParametersStep") // rename to collect system parameters and allocate ports?
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CollectSystemParametersStep extends SyncActivitiStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private Configuration configuration;

    protected Supplier<CredentialsGenerator> credentialsGeneratorSupplier = () -> new CredentialsGenerator();
    protected Supplier<String> timestampSupplier = () -> new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());

    protected ExecutionStatus executeStep(ExecutionWrapper execution) throws SLException {
        return executeStepInternal(execution, false);
    }

    protected ExecutionStatus executeStepInternal(ExecutionWrapper execution, boolean reserveTemporaryRoute) throws SLException {
        getStepLogger().logActivitiTask();

        getStepLogger().info(Messages.COLLECTING_SYSTEM_PARAMETERS);
        PortAllocator portAllocator = null;
        try {

            CloudFoundryOperations client = execution.getCloudFoundryClient();
            String defaultDomainName = getDefaultDomain(client);
            getStepLogger().debug(Messages.DEFAULT_DOMAIN, defaultDomainName);
            boolean portBasedRouting = isPortBasedRouting(client);
            getStepLogger().debug(Messages.PORT_BASED_ROUTING, portBasedRouting);
            if (portBasedRouting) {
                portAllocator = clientProvider.getPortAllocator(client, defaultDomainName);
            }

            SystemParametersBuilder systemParametersBuilder = createParametersBuilder(execution.getContext(), client, portAllocator,
                portBasedRouting, defaultDomainName, reserveTemporaryRoute);
            DeploymentDescriptor descriptor = StepsUtil.getUnresolvedDeploymentDescriptor(execution.getContext());
            SystemParameters systemParameters = systemParametersBuilder.build(descriptor);
            getStepLogger().debug(Messages.SYSTEM_PARAMETERS, secureSerializer.toJson(systemParameters));

            determineIsVersionAccepted(execution.getContext(), descriptor, portAllocator);

            if (portBasedRouting) {
                StepsUtil.setAllocatedPorts(execution.getContext(), portAllocator.getAllocatedPorts());
                getStepLogger().debug(Messages.ALLOCATED_PORTS, portAllocator.getAllocatedPorts());
            }
            execution.getContext().setVariable(Constants.VAR_PORT_BASED_ROUTING, portBasedRouting);

            StepsUtil.setSystemParameters(execution.getContext(), systemParameters);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            cleanUp(portAllocator);
            getStepLogger().error(e, Messages.ERROR_COLLECTING_SYSTEM_PARAMETERS);
            throw e;
        } catch (SLException e) {
            cleanUp(portAllocator);
            getStepLogger().error(e, Messages.ERROR_COLLECTING_SYSTEM_PARAMETERS);
            throw e;
        }
        getStepLogger().debug(Messages.SYSTEM_PARAMETERS_COLLECTED);

        return ExecutionStatus.SUCCESS;
    }

    private String getDefaultDomain(CloudFoundryOperations client) {
        CloudDomain defaultDomain = client.getDefaultDomain();
        if (defaultDomain != null) {
            return defaultDomain.getName();
        }
        return null;
    }

    private boolean isPortBasedRouting(CloudFoundryOperations client) {
        CloudInfo info = client.getCloudInfo();
        if (info instanceof CloudInfoExtended) {
            return ((CloudInfoExtended) info).isPortBasedRouting();
        }
        return false;
    }

    private SystemParametersBuilder createParametersBuilder(DelegateExecution context, CloudFoundryOperations client,
        PortAllocator portAllocator, boolean portBasedRouting, String defaultDomainName, boolean reserveTemporaryRoute) {
        DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
        String platformName = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_TARGET_NAME);
        boolean useNamespacesForServices = (boolean) context.getVariable(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES);
        int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
        boolean useNamespaces = (boolean) context.getVariable(Constants.PARAM_USE_NAMESPACES);

        String authorizationEndpoint = client.getCloudInfo().getAuthorizationEndpoint();
        int routerPort = configuration.getRouterPort();
        String user = (String) context.getVariable(Constants.VAR_USER);

        URL targetUrl = configuration.getTargetURL();

        String deployServiceUrl = getDeployServiceUrl(client);
        Map<String, Object> xsPlaceholderReplacementValues = buildXsPlaceholderReplacementValues(defaultDomainName, authorizationEndpoint,
            deployServiceUrl, routerPort, targetUrl.toString(), targetUrl.getProtocol());
        StepsUtil.setXsPlaceholderReplacementValues(context, xsPlaceholderReplacementValues);
        XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);

        boolean areXsPlaceholdersSupported = configuration.areXsPlaceholdersSupported();

        SystemParametersBuilder systemParametersBuilder = new SystemParametersBuilder(platformName, StepsUtil.getOrg(context),
            StepsUtil.getSpace(context), user, defaultDomainName, configuration.getPlatformType(), targetUrl, authorizationEndpoint,
            deployServiceUrl, routerPort, portBasedRouting, reserveTemporaryRoute, portAllocator, useNamespaces, useNamespacesForServices,
            deployedMta, credentialsGeneratorSupplier.get(), majorSchemaVersion, areXsPlaceholdersSupported, xsPlaceholderResolver,
            timestampSupplier);
        return systemParametersBuilder;
    }

    private Map<String, Object> buildXsPlaceholderReplacementValues(String defaultDomain, String authorizationEndpoint,
        String deployServiceUrl, int routerPort, String controllerEndpoint, String protocol) {
        Map<String, Object> result = new TreeMap<>();
        result.put(SupportedParameters.XSA_CONTROLLER_ENDPOINT_PLACEHOLDER, controllerEndpoint);
        result.put(SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER, routerPort);
        result.put(SupportedParameters.XSA_AUTHORIZATION_ENDPOINT_PLACEHOLDER, authorizationEndpoint);
        result.put(SupportedParameters.XSA_DEFAULT_DOMAIN_PLACEHOLDER, defaultDomain);
        result.put(SupportedParameters.XSA_DEPLOY_SERVICE_URL_PLACEHOLDER, deployServiceUrl);
        result.put(SupportedParameters.XSA_PROTOCOL_PLACEHOLDER, protocol);
        return result;
    }

    private String getDeployServiceUrl(CloudFoundryOperations client) {
        CloudInfo info = client.getCloudInfo();
        if (info instanceof CloudInfoExtended) {
            return ((CloudInfoExtended) info).getDeployServiceUrl();
        }
        return configuration.getDeployServiceUrl();
    }

    private void determineIsVersionAccepted(DelegateExecution context, DeploymentDescriptor descriptor, PortAllocator portAllocator) {
        DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
        VersionRule versionRule = VersionRule.valueOf((String) context.getVariable(Constants.PARAM_VERSION_RULE));
        getStepLogger().debug(Messages.VERSION_RULE, versionRule);

        Version mtaVersion = Version.parseVersion(descriptor.getVersion());
        getStepLogger().info(Messages.NEW_MTA_VERSION, mtaVersion);
        boolean mtaVersionAccepted = isVersionAccepted(versionRule, deployedMta, mtaVersion);
        if (!mtaVersionAccepted) {
            cleanUp(portAllocator);
            throw new SLException(Messages.MTA_VERSION_REJECTED, versionRule, versionRule.getErrorMessage());
        } else {
            getStepLogger().debug(Messages.MTA_VERSION_ACCEPTED);
        }
        StepsUtil.setMtaVersionAccepted(context, mtaVersionAccepted);
    }

    private boolean isVersionAccepted(VersionRule versionRule, DeployedMta deployedMta, Version newMtaVersion) {
        if (deployedMta == null) {
            return true;
        }
        if (deployedMta.getMetadata().isVersionUnknown()) {
            getStepLogger().warn(Messages.IGNORING_VERSION_RULE);
            return true;
        }
        Version deployedMtaVersion = deployedMta.getMetadata().getVersion();
        getStepLogger().info(Messages.DEPLOYED_MTA_VERSION, deployedMtaVersion);
        return versionRule.accept(newMtaVersion, deployedMtaVersion);
    }

    private void cleanUp(PortAllocator portAllocator) {
        if (portAllocator != null) {
            portAllocator.freeAll();
        }
    }

}
