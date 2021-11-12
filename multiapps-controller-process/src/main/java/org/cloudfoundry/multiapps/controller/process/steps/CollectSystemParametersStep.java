package org.cloudfoundry.multiapps.controller.process.steps;

import java.net.URL;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.ContentException;
import com.sap.cloudfoundry.client.facade.util.AuthorizationEndpointGetter;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.CredentialsGenerator;
import org.cloudfoundry.multiapps.controller.core.helpers.SystemParameters;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.HostValidator;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ReadOnlyParametersChecker;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.DeploymentType;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.cloudfoundry.multiapps.mta.model.VersionRule;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

@Named("collectSystemParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CollectSystemParametersStep extends SyncFlowableStep {

    private static final String DEFAULT_DOMAIN_PLACEHOLDER = "apps.internal";

    @Inject
    private ReadOnlyParametersChecker readOnlyParametersChecker;

    protected Supplier<CredentialsGenerator> credentialsGeneratorSupplier = CredentialsGenerator::new;
    protected Supplier<String> timestampSupplier = () -> DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                                                                          .format(ZonedDateTime.now());

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        return executeStepInternal(context, false);
    }

    protected StepPhase executeStepInternal(ProcessContext context, boolean reserveTemporaryRoutes) {
        getStepLogger().debug(Messages.COLLECTING_SYSTEM_PARAMETERS);
        CloudControllerClient client = context.getControllerClient();
        String defaultDomainName = getDefaultDomain(client, context);
        getStepLogger().debug(Messages.DEFAULT_DOMAIN, defaultDomainName);

        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        checkForOverwrittenReadOnlyParameters(descriptor);
        SystemParameters systemParameters = createSystemParameters(context, client, defaultDomainName, reserveTemporaryRoutes);
        systemParameters.injectInto(descriptor);
        getStepLogger().debug(Messages.DESCRIPTOR_WITH_SYSTEM_PARAMETERS, SecureSerialization.toJson(descriptor));

        determineIsVersionAccepted(context, descriptor);

        context.setVariable(Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS, descriptor);
        context.setVariable(Variables.START_APPS, shouldStartApplications(context));
        getStepLogger().debug(Messages.SYSTEM_PARAMETERS_COLLECTED);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_COLLECTING_SYSTEM_PARAMETERS;
    }

    private String getDefaultDomain(CloudControllerClient client, ProcessContext context) {
        try {
            return client.getDefaultDomain()
                         .getName();
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                context.setVariable(Variables.MISSING_DEFAULT_DOMAIN, true);
                return DEFAULT_DOMAIN_PLACEHOLDER;
            }
            throw e;
        }
    }

    private void checkForOverwrittenReadOnlyParameters(DeploymentDescriptor descriptor) {
        getStepLogger().debug(MessageFormat.format(Messages.CHECKING_FOR_OVERWRITING_READ_ONLY_PARAMETERS, descriptor.getId()));
        readOnlyParametersChecker.check(descriptor);
        getStepLogger().debug(Messages.NO_READ_ONLY_PARAMETERS_ARE_OVERWRITTEN);
    }

    private SystemParameters createSystemParameters(ProcessContext context, CloudControllerClient client, String defaultDomain,
                                                    boolean reserveTemporaryRoutes) {
        String authorizationEndpoint = getAuthorizationEndpointGetter(client).getAuthorizationEndpoint();
        String user = context.getVariable(Variables.USER);
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);
        boolean applyNamespace = context.getVariable(Variables.APPLY_NAMESPACE);

        URL controllerUrl = configuration.getControllerUrl();
        String deployServiceUrl = configuration.getDeployServiceUrl();

        return new SystemParameters.Builder().organizationName(context.getVariable(Variables.ORGANIZATION_NAME))
                                             .organizationGuid(context.getVariable(Variables.ORGANIZATION_GUID))
                                             .spaceName(context.getVariable(Variables.SPACE_NAME))
                                             .spaceGuid(context.getVariable(Variables.SPACE_GUID))
                                             .user(user)
                                             .defaultDomain(defaultDomain)
                                             .controllerUrl(controllerUrl)
                                             .authorizationEndpoint(authorizationEndpoint)
                                             .deployServiceUrl(deployServiceUrl)
                                             .reserveTemporaryRoutes(reserveTemporaryRoutes)
                                             .credentialsGenerator(credentialsGeneratorSupplier.get())
                                             .timestampSupplier(timestampSupplier)
                                             .hostValidator(new HostValidator(namespace, applyNamespace))
                                             .build();
    }

    private void determineIsVersionAccepted(ProcessContext context, DeploymentDescriptor descriptor) {
        DeployedMta deployedMta = context.getVariable(Variables.DEPLOYED_MTA);
        VersionRule versionRule = context.getVariable(Variables.VERSION_RULE);
        getStepLogger().debug(Messages.VERSION_RULE, versionRule);

        Version mtaVersion = Version.parseVersion(descriptor.getVersion());
        getStepLogger().info(Messages.DETECTED_NEW_MTA_VERSION, mtaVersion);
        DeploymentType deploymentType = getDeploymentType(deployedMta, mtaVersion);
        if (versionRule.allows(deploymentType)) {
            getStepLogger().debug(Messages.MTA_VERSION_ACCEPTED);
            return;
        }
        if (deploymentType == DeploymentType.DOWNGRADE) {
            throw new ContentException(Messages.HIGHER_VERSION_ALREADY_DEPLOYED);
        }
        if (deploymentType == DeploymentType.REDEPLOYMENT) {
            throw new ContentException(Messages.SAME_VERSION_ALREADY_DEPLOYED);
        }
        throw new IllegalStateException(MessageFormat.format(Messages.VERSION_RULE_DOES_NOT_ALLOW_DEPLOYMENT_TYPE, versionRule,
                                                             deploymentType));
    }

    private DeploymentType getDeploymentType(DeployedMta deployedMta, Version newMtaVersion) {
        if (deployedMta == null) {
            return DeploymentType.DEPLOYMENT;
        }
        if (deployedMta.getMetadata()
                       .getVersion() == null) {
            getStepLogger().warn(Messages.IGNORING_VERSION_RULE);
            return DeploymentType.UPGRADE;
        }
        Version deployedMtaVersion = deployedMta.getMetadata()
                                                .getVersion();
        getStepLogger().info(Messages.DEPLOYED_MTA_VERSION, deployedMtaVersion);
        return DeploymentType.fromVersions(deployedMtaVersion, newMtaVersion);
    }

    protected boolean shouldStartApplications(ProcessContext context) {
        return true;
    }

    protected AuthorizationEndpointGetter getAuthorizationEndpointGetter(CloudControllerClient client) {
        return new AuthorizationEndpointGetter(new WebClientFactory().getWebClient(client));
    }

}
