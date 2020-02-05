package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.helpers.ApplicationColorDetector;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ApplicationProductizationStateUpdaterBasedOnColor;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;

@Named("blueGreenRenameStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BlueGreenRenameStep extends SyncFlowableStep {

    private static final ApplicationColor DEFAULT_MTA_COLOR = ApplicationColor.BLUE;

    @Inject
    protected ApplicationColorDetector applicationColorDetector;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DETECTING_COLOR_OF_DEPLOYED_MTA);
        DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptor(execution.getContext());
        DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());
        ApplicationColor idleMtaColor = DEFAULT_MTA_COLOR;
        ApplicationColor liveMtaColor = null;
        try {
            liveMtaColor = applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta);
            if (liveMtaColor != null) {
                getStepLogger().info(Messages.DEPLOYED_MTA_COLOR, liveMtaColor);
                idleMtaColor = liveMtaColor.getAlternativeColor();
            }
        } catch (ConflictException e) {
            getStepLogger().warn(e.getMessage());
            liveMtaColor = getLiveApplicationColor(deployedMta, execution);
            idleMtaColor = liveMtaColor.getAlternativeColor();
            getStepLogger().info(Messages.ASSUMED_LIVE_AND_IDLE_COLORS, liveMtaColor, idleMtaColor);
        }

        setIdleApplications(execution, deployedMta, liveMtaColor);
        StepsUtil.setLiveMtaColor(execution.getContext(), liveMtaColor);
        StepsUtil.setIdleMtaColor(execution.getContext(), idleMtaColor);

        getStepLogger().info(Messages.NEW_MTA_COLOR, idleMtaColor);

        visit(execution, descriptor, idleMtaColor, liveMtaColor);

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_RENAMING_APPLICATIONS;
    }

    private ApplicationColor getLiveApplicationColor(DeployedMta deployedMta, ExecutionWrapper execution) {
        return applicationColorDetector.detectLiveApplicationColor(deployedMta, (String) execution.getContext()
                                                                                                  .getVariable(Constants.VAR_CORRELATION_ID));
    }

    private void setIdleApplications(ExecutionWrapper execution, DeployedMta deployedMta, ApplicationColor liveMtaColor) {
        if (deployedMta != null && liveMtaColor != null) {
            List<DeployedMtaApplication> updatedApplications = new ApplicationProductizationStateUpdaterBasedOnColor(getStepLogger(),
                                                                                                                     liveMtaColor).updateApplicationsProductizationState(deployedMta.getApplications());
            DeployedMta mtaWithUpdatedApplications = ImmutableDeployedMta.builder()
                                                                         .from(deployedMta)
                                                                         .applications(updatedApplications)
                                                                         .build();
            StepsUtil.setDeployedMta(execution.getContext(), mtaWithUpdatedApplications);
        }
    }

    protected void visit(ExecutionWrapper execution, DeploymentDescriptor descriptor, ApplicationColor mtaColor,
                         ApplicationColor deployedMtaColor) {
        ApplicationColorAppender appender = new ApplicationColorAppender(deployedMtaColor, mtaColor);
        descriptor.accept(appender);
        StepsUtil.setDeploymentDescriptor(execution.getContext(), descriptor);
    }

}
