package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.helpers.CredentialsGenerator;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.SystemParametersBuilder;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.mta.model.VersionRule;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("collectSystemParametersStep") // rename to collect system parameters and allocate ports?
public class CollectSystemParametersStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectSystemParametersStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("collectSystemParametersTask").displayName("Collect System Parameters").description(
            "Collect System Parameters").build();
    }

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    protected Supplier<CredentialsGenerator> credentialsGeneratorSupplier = () -> new CredentialsGenerator();
    protected Supplier<PlatformType> platformTypeSupplier = () -> ConfigurationUtil.getPlatformType();
    protected Supplier<Boolean> areXsPlaceholdersSupportedSupplier = () -> ConfigurationUtil.areXsPlaceholdersSupported();
    protected Supplier<String> timestampSupplier = () -> new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime());

    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        return executeStepInternal(context, false);
    }

    protected ExecutionStatus executeStepInternal(DelegateExecution context, boolean reserveTemporaryRoute) throws SLException {
        logActivitiTask(context, LOGGER);

        info(context, Messages.COLLECTING_SYSTEM_PARAMETERS, LOGGER);
        PortAllocator portAllocator = null;
        try {

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);
            String defaultDomainName = getDefaultDomain(client);
            debug(context, format(Messages.DEFAULT_DOMAIN, defaultDomainName), LOGGER);
            boolean portBasedRouting = isPortBasedRouting(client);
            if (portBasedRouting) {
                portAllocator = clientProvider.getPortAllocator(client, defaultDomainName);
            }

            SystemParametersBuilder systemParametersBuilder = createParametersBuilder(context, client, portAllocator, portBasedRouting,
                defaultDomainName, reserveTemporaryRoute);
            DeploymentDescriptor descriptor = StepsUtil.getUnresolvedDeploymentDescriptor(context);
            SystemParameters systemParameters = systemParametersBuilder.build(descriptor);
            debug(context, format(Messages.SYSTEM_PARAMETERS, secureSerializer.toJson(systemParameters)), LOGGER);

            determineIsVersionAccepted(context, descriptor, portAllocator);

            if (portBasedRouting) {
                StepsUtil.setAllocatedPorts(context, portAllocator.getAllocatedPorts());
                debug(context, format(Messages.ALLOCATED_PORTS, portAllocator.getAllocatedPorts()), LOGGER);
            }
            context.setVariable(Constants.VAR_PORT_BASED_ROUTING, portBasedRouting);

            StepsUtil.setSystemParameters(context, systemParameters);
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            cleanUp(portAllocator);
            error(context, Messages.ERROR_COLLECTING_SYSTEM_PARAMETERS, e, LOGGER);
            throw e;
        } catch (SLException e) {
            cleanUp(portAllocator);
            error(context, Messages.ERROR_COLLECTING_SYSTEM_PARAMETERS, e, LOGGER);
            throw e;
        }
        debug(context, Messages.SYSTEM_PARAMETERS_COLLECTED, LOGGER);

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
        int routerPort = ConfigurationUtil.getRouterPort();
        String user = (String) context.getVariable(Constants.VAR_USER);

        URL targetUrl = ConfigurationUtil.getTargetURL();

        String deployServiceUrl = getDeployServiceUrl(client);
        Map<String, Object> xsPlaceholderReplacementValues = buildXsPlaceholderReplacementValues(defaultDomainName, authorizationEndpoint,
            deployServiceUrl, routerPort, targetUrl.toString(), targetUrl.getProtocol());
        StepsUtil.setXsPlaceholderReplacementValues(context, xsPlaceholderReplacementValues);
        XsPlaceholderResolver xsPlaceholderResolver = StepsUtil.getXsPlaceholderResolver(context);

        boolean areXsPlaceholdersSupported = areXsPlaceholdersSupportedSupplier.get();

        SystemParametersBuilder systemParametersBuilder = new SystemParametersBuilder(platformName, StepsUtil.getOrg(context),
            StepsUtil.getSpace(context), user, defaultDomainName, platformTypeSupplier.get(), targetUrl, authorizationEndpoint,
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
        return ConfigurationUtil.getDeployServiceUrl();
    }

    private void determineIsVersionAccepted(DelegateExecution context, DeploymentDescriptor descriptor, PortAllocator portAllocator) {
        DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
        VersionRule versionRule = VersionRule.valueOf((String) context.getVariable(Constants.PARAM_VERSION_RULE));
        debug(context, format(Messages.VERSION_RULE, versionRule), LOGGER);

        Version mtaVersion = Version.parseVersion(descriptor.getVersion());
        info(context, format(Messages.NEW_MTA_VERSION, mtaVersion), LOGGER);
        boolean mtaVersionAccepted = isVersionAccepted(context, versionRule, deployedMta, mtaVersion);
        if (!mtaVersionAccepted) {
            cleanUp(portAllocator);
            throw new SLException(format(Messages.MTA_VERSION_REJECTED, versionRule, versionRule.getErrorMessage()));
        } else {
            debug(context, Messages.MTA_VERSION_ACCEPTED, LOGGER);
        }
        StepsUtil.setMtaVersionAccepted(context, mtaVersionAccepted);
    }

    private boolean isVersionAccepted(DelegateExecution context, VersionRule versionRule, DeployedMta deployedMta, Version newMtaVersion) {
        if (deployedMta == null) {
            return true;
        }
        if (deployedMta.getMetadata().isVersionUnknown()) {
            warn(context, Messages.IGNORING_VERSION_RULE, LOGGER);
            return true;
        }
        Version deployedMtaVersion = deployedMta.getMetadata().getVersion();
        info(context, format(Messages.DEPLOYED_MTA_VERSION, deployedMtaVersion), LOGGER);
        return versionRule.accept(newMtaVersion, deployedMtaVersion);
    }

    private void cleanUp(PortAllocator portAllocator) {
        if (portAllocator != null) {
            portAllocator.freeAll();
        }
    }

}
