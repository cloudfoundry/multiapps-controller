package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.function.Supplier;

import javax.inject.Named;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationColorDetector;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;

@Named("blueGreenRenameStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BlueGreenRenameStep extends SyncFlowableStep {

    private static final ApplicationColor DEFAULT_MTA_COLOR = ApplicationColor.BLUE;

    protected Supplier<ApplicationColorDetector> colorDetectorSupplier = ApplicationColorDetector::new;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        try {
            getStepLogger().debug(Messages.DETECTING_COLOR_OF_DEPLOYED_MTA);

            DeploymentDescriptor descriptor = StepsUtil.getUnresolvedDeploymentDescriptor(execution.getContext());
            DeployedMta deployedMta = StepsUtil.getDeployedMta(execution.getContext());

            ApplicationColorDetector detector = colorDetectorSupplier.get();
            ApplicationColor mtaColor;
            ApplicationColor deployedMtaColor = null;
            try {
                deployedMtaColor = detector.detectSingularDeployedApplicationColor(deployedMta);
                if (deployedMtaColor != null) {
                    getStepLogger().info(Messages.DEPLOYED_MTA_COLOR, deployedMtaColor);
                    mtaColor = deployedMtaColor.getAlternativeColor();
                } else {
                    mtaColor = DEFAULT_MTA_COLOR;
                }
            } catch (ConflictException e) {
                getStepLogger().warn(e.getMessage());
                // Assume that the last deployed color was not deployed successfully and try to update (fix) it in this process:
                ApplicationColor liveMtaColor = detector.detectFirstDeployedApplicationColor(deployedMta);
                ApplicationColor idleMtaColor = liveMtaColor.getAlternativeColor();
                getStepLogger().info(Messages.ASSUMED_LIVE_AND_IDLE_COLORS, liveMtaColor, idleMtaColor);
                mtaColor = idleMtaColor;
            }

            StepsUtil.setDeployedMtaColor(execution.getContext(), deployedMtaColor);
            StepsUtil.setMtaColor(execution.getContext(), mtaColor);

            getStepLogger().info(Messages.NEW_MTA_COLOR, mtaColor);

            visit(execution, descriptor, mtaColor, deployedMtaColor);

            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_RENAMING_MODULES);
            throw e;
        }
    }

    protected void visit(ExecutionWrapper execution, DeploymentDescriptor descriptor, ApplicationColor mtaColor,
        ApplicationColor deployedMtaColor) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(execution.getContext());
        ApplicationColorAppender appender = handlerFactory.getApplicationColorAppender(deployedMtaColor, mtaColor);
        descriptor.accept(appender);
        StepsUtil.setUnresolvedDeploymentDescriptor(execution.getContext(), descriptor);
    }

}
