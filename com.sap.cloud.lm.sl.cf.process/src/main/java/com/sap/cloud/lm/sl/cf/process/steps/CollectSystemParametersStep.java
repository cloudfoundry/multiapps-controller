package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudInfoExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientFactory.PlatformType;
import com.sap.cloud.lm.sl.cf.core.helpers.CredentialsGenerator;
import com.sap.cloud.lm.sl.cf.core.helpers.OccupiedPortsDetector;
import com.sap.cloud.lm.sl.cf.core.helpers.PortAllocator;
import com.sap.cloud.lm.sl.cf.core.helpers.SystemParametersBuilder;
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

@Component("collectSystemParametersStep")
public class CollectSystemParametersStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectSystemParametersStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("collectSystemParametersTask", "Collect System Parameters", "Collect System Parameters");
    }

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    protected Supplier<CredentialsGenerator> credentialsGeneratorSupplier = () -> new CredentialsGenerator();
    protected Supplier<PlatformType> platformTypeSupplier = () -> ConfigurationUtil.getPlatformType();
    protected Supplier<Boolean> areXsPlaceholdersSupportedSupplier = () -> ConfigurationUtil.areXsPlaceholdersSupported();

    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        return executeStepInternal(context, false);
    }

    protected ExecutionStatus executeStepInternal(DelegateExecution context, boolean reserveTemporaryRoute) throws SLException {
        logActivitiTask(context, LOGGER);

        info(context, Messages.COLLECTING_SYSTEM_PARAMETERS, LOGGER);
        PortAllocator portAllocator = null;
        try {
            String platformName = StepsUtil.getRequiredStringParameter(context, Constants.PARAM_PLATFORM_NAME);
            boolean useNamespacesForServices = ContextUtil.getVariable(context, Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, false);
            int majorSchemaVersion = (int) context.getVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION);
            boolean useNamespaces = ContextUtil.getVariable(context, Constants.PARAM_USE_NAMESPACES, true);

            CloudFoundryOperations client = getCloudFoundryClient(context, LOGGER);
            List<CloudApplication> deployedApps = StepsUtil.getDeployedApps(context);
            DeployedMta deployedMta = StepsUtil.getDeployedMta(context);

            String defaultDomainName = getDefaultDomain(client);
            debug(context, format(Messages.DEFAULT_DOMAIN, defaultDomainName), LOGGER);
            boolean portBasedRouting = isPortBasedRouting(client);
            String authorizationEndpoint = client.getCloudInfo().getAuthorizationEndpoint();
            int routerPort = ConfigurationUtil.getRouterPort();
            String user = ContextUtil.getVariable(context, Constants.VAR_USER, null);
            String deployServiceUrl = getDeployServiceUrl(client);

            if (portBasedRouting) {
                portAllocator = clientProvider.getPortAllocator(client, defaultDomainName);
            }

            DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptor(context);

            URL targetUrl = ConfigurationUtil.getTargetURL();

            Map<String, Object> xsPlaceholderReplacementValues = buildXsPlaceholderReplacementValues(defaultDomainName,
                authorizationEndpoint, deployServiceUrl, routerPort, targetUrl.toString(), targetUrl.getProtocol());
            StepsUtil.setXsPlaceholderReplacementValues(context, xsPlaceholderReplacementValues);

            Map<String, List<Integer>> occupiedPorts = getOccupiedPorts(deployedApps, portBasedRouting);

            boolean areXsPlaceholdersSupported = areXsPlaceholdersSupportedSupplier.get();

            SystemParametersBuilder systemParametersBuilder = new SystemParametersBuilder(platformName, StepsUtil.getOrg(context),
                StepsUtil.getSpace(context), user, defaultDomainName, platformTypeSupplier.get(), targetUrl, authorizationEndpoint,
                deployServiceUrl, routerPort, portBasedRouting, reserveTemporaryRoute, portAllocator, occupiedPorts, useNamespaces,
                useNamespacesForServices, deployedMta, credentialsGeneratorSupplier.get(), majorSchemaVersion, areXsPlaceholdersSupported);

            SystemParameters systemParameters = systemParametersBuilder.build(descriptor);
            debug(context, format(Messages.SYSTEM_PARAMETERS, secureSerializer.toJson(systemParameters)), LOGGER);

            determineShouldVersionBeAcceptedForDeployment(context, descriptor, deployedMta, portAllocator);

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

    private void determineShouldVersionBeAcceptedForDeployment(DelegateExecution context, DeploymentDescriptor descriptor,
        DeployedMta deployedMta, PortAllocator portAllocator) {
        VersionRule versionRule = VersionRule.valueOf((String) context.getVariable(Constants.PARAM_VERSION_RULE));
        debug(context, format(Messages.VERSION_RULE, versionRule), LOGGER);

        Version mtaVersion = Version.parseVersion(descriptor.getVersion());
        boolean mtaVersionAccepted;
        if (deployedMta != null) {
            if (!deployedMta.getMetadata().isVersionUnknown()) {
                Version deployedMtaVersion = deployedMta.getMetadata().getVersion();
                info(context, format(Messages.DEPLOYED_MTA_VERSION, deployedMtaVersion), LOGGER);
                mtaVersionAccepted = versionRule.accept(mtaVersion, deployedMtaVersion);
            } else {
                warn(context, Messages.IGNORING_VERSION_RULE, LOGGER);
                mtaVersionAccepted = true;
            }
        } else {
            mtaVersionAccepted = true;
        }
        info(context, format(Messages.NEW_MTA_VERSION, mtaVersion), LOGGER);
        if (mtaVersionAccepted) {
            debug(context, Messages.MTA_VERSION_ACCEPTED, LOGGER);
        } else {
            info(context, format(Messages.MTA_VERSION_REJECTED, versionRule), LOGGER);
            cleanUp(portAllocator);
        }
        StepsUtil.setMtaVersionAccepted(context, mtaVersionAccepted);
    }

    private Map<String, List<Integer>> getOccupiedPorts(List<CloudApplication> apps, boolean portBasedRouting) {
        if (portBasedRouting) {
            return new OccupiedPortsDetector().detectOccupiedPorts(apps);
        }
        return Collections.emptyMap();
    }

    private boolean isPortBasedRouting(CloudFoundryOperations client) {
        CloudInfo info = client.getCloudInfo();
        if (info instanceof CloudInfoExtended) {
            return ((CloudInfoExtended) info).isPortBasedRouting();
        }
        return false;
    }

    private String getDeployServiceUrl(CloudFoundryOperations client) {
        CloudInfo info = client.getCloudInfo();
        if (info instanceof CloudInfoExtended) {
            return ((CloudInfoExtended) info).getDeployServiceUrl();
        }
        return ConfigurationUtil.getDeployServiceUrl();
    }

    private String getDefaultDomain(CloudFoundryOperations client) {
        CloudDomain defaultDomain = client.getDefaultDomain();
        if (defaultDomain != null) {
            return defaultDomain.getName();
        }
        return null;
    }

    private void cleanUp(PortAllocator portAllocator) {
        if (portAllocator != null) {
            portAllocator.freeAll();
        }
    }

}
