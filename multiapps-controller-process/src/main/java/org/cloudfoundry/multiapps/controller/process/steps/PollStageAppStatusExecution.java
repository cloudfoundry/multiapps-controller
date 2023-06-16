package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationStager;
import org.cloudfoundry.multiapps.controller.process.util.StagingState;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.PackageState;

public class PollStageAppStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollStageAppStatusExecution.class);

    private final ApplicationStager applicationStager;
    private final CloudControllerClientFactory clientFactory;
    private final TokenService tokenService;

    public PollStageAppStatusExecution(ApplicationStager applicationStager, CloudControllerClientFactory clientFactory,
                                       TokenService tokenService) {
        this.applicationStager = applicationStager;
        this.clientFactory = clientFactory;
        this.tokenService = tokenService;
    }

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudApplication application = context.getVariable(Variables.APP_TO_PROCESS);
        CloudControllerClient client = context.getControllerClient();
        StepLogger stepLogger = context.getStepLogger();
        stepLogger.debug(Messages.CHECKING_APP_STATUS, application.getName());

        StagingState state = applicationStager.getStagingState();
        stepLogger.debug(Messages.APP_STAGING_STATUS, application.getName(), state.getState());

        ProcessLoggerProvider processLoggerProvider = stepLogger.getProcessLoggerProvider();

        var user = context.getVariable(Variables.USER);
        var correlationId = context.getVariable(Variables.CORRELATION_ID);
        var logCacheClient = clientFactory.createLogCacheClient(tokenService.getToken(user), correlationId);

        UUID appGuid = client.getApplicationGuid(application.getName());
        StepsUtil.saveAppLogs(context, logCacheClient, appGuid, application.getName(), LOGGER, processLoggerProvider);

        if (state.getState() != PackageState.STAGED) {
            return checkStagingState(context.getStepLogger(), application.getName(), state);
        }
        applicationStager.bindDropletToApplication(appGuid);
        stepLogger.info(Messages.APP_STAGED, application.getName());
        return AsyncExecutionState.FINISHED;
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        CloudApplication application = context.getVariable(Variables.APP_TO_PROCESS);
        return MessageFormat.format(Messages.ERROR_STAGING_APP_0, application.getName());
    }

    private AsyncExecutionState checkStagingState(StepLogger stepLogger, String appName, StagingState state) {
        if (state.getState() == PackageState.FAILED) {
            stepLogger.error(Messages.ERROR_STAGING_APP_0_DESCRIPTION_1, appName, state.getError());
            return AsyncExecutionState.ERROR;
        }
        return AsyncExecutionState.RUNNING;
    }

}
