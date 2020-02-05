package com.sap.cloud.lm.sl.cf.process.steps;

import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.helpers.CredentialsGenerator;
import com.sap.cloud.lm.sl.cf.core.helpers.SystemParameters;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ReadOnlyParametersChecker;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.DeploymentType;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.mta.model.VersionRule;

@Named("collectSystemParametersStep") // rename to collect system parameters and allocate ports?
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CollectSystemParametersStep extends SyncFlowableStep {

    private final SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private ReadOnlyParametersChecker readOnlyParametersChecker;

    protected Supplier<CredentialsGenerator> credentialsGeneratorSupplier = CredentialsGenerator::new;
    protected Supplier<String> timestampSupplier = () -> new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance()
                                                                                                               .getTime());

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        return executeStepInternal(execution, false);
    }

    protected StepPhase executeStepInternal(ExecutionWrapper execution, boolean reserveTemporaryRoutes) {
        getStepLogger().debug(Messages.COLLECTING_SYSTEM_PARAMETERS);
        CloudControllerClient client = execution.getControllerClient();
        String defaultDomainName = getDefaultDomain(client);
        getStepLogger().debug(Messages.DEFAULT_DOMAIN, defaultDomainName);

        DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptor(execution.getContext());
        checkForOverwrittenReadOnlyParameters(descriptor);
        SystemParameters systemParameters = createSystemParameters(execution.getContext(), client, defaultDomainName,
                                                                   reserveTemporaryRoutes);
        systemParameters.injectInto(descriptor);
        getStepLogger().debug(Messages.DESCRIPTOR_WITH_SYSTEM_PARAMETERS, secureSerializer.toJson(descriptor));

        determineIsVersionAccepted(execution.getContext(), descriptor);

        StepsUtil.setDeploymentDescriptorWithSystemParameters(execution.getContext(), descriptor);
        getStepLogger().debug(Messages.SYSTEM_PARAMETERS_COLLECTED);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_COLLECTING_SYSTEM_PARAMETERS;
    }

    private String getDefaultDomain(CloudControllerClient client) {
        CloudDomain defaultDomain = client.getDefaultDomain();
        if (defaultDomain != null) {
            return defaultDomain.getName();
        }
        return null;
    }

    private void checkForOverwrittenReadOnlyParameters(DeploymentDescriptor descriptor) {
        getStepLogger().debug(MessageFormat.format(Messages.CHECKING_FOR_OVERWRITING_READ_ONLY_PARAMETERS, descriptor.getId()));
        readOnlyParametersChecker.check(descriptor);
        getStepLogger().debug(Messages.NO_READ_ONLY_PARAMETERS_ARE_OVERWRITTEN);
    }

    private SystemParameters createSystemParameters(DelegateExecution context, CloudControllerClient client, String defaultDomain,
                                                    boolean reserveTemporaryRoutes) {
        String authorizationEndpoint = client.getCloudInfo()
                                             .getAuthorizationEndpoint();
        String user = (String) context.getVariable(Constants.VAR_USER);

        URL controllerUrl = configuration.getControllerUrl();
        String deployServiceUrl = configuration.getDeployServiceUrl();

        return new SystemParameters.Builder().organization(StepsUtil.getOrg(context))
                                             .space(StepsUtil.getSpace(context))
                                             .user(user)
                                             .defaultDomain(defaultDomain)
                                             .controllerUrl(controllerUrl)
                                             .authorizationEndpoint(authorizationEndpoint)
                                             .deployServiceUrl(deployServiceUrl)
                                             .reserveTemporaryRoutes(reserveTemporaryRoutes)
                                             .credentialsGenerator(credentialsGeneratorSupplier.get())
                                             .timestampSupplier(timestampSupplier)
                                             .build();
    }

    private void determineIsVersionAccepted(DelegateExecution context, DeploymentDescriptor descriptor) {
        DeployedMta deployedMta = StepsUtil.getDeployedMta(context);
        VersionRule versionRule = VersionRule.valueOf((String) context.getVariable(Constants.PARAM_VERSION_RULE));
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

}
