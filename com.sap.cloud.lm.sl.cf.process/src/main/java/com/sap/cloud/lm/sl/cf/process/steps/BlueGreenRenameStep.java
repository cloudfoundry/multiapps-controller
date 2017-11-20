package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.function.Supplier;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationColorDetector;
import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ApplicationColorAppender;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;

public class BlueGreenRenameStep extends SyncActivitiStep {

    private static final ApplicationColor DEFAULT_MTA_COLOR = ApplicationColor.BLUE;

    protected Supplier<ApplicationColorDetector> colorDetectorSupplier = () -> new ApplicationColorDetector();

    @Override
    protected ExecutionStatus executeStep(ExecutionWrapper execution) throws SLException {
        getStepLogger().logActivitiTask();

        try {
            getStepLogger().info(Messages.DETECTING_COLOR_OF_DEPLOYED_MTA);

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
                    execution.getContext().setVariable("deployedMtaColor", deployedMtaColor);
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
            getStepLogger().info(Messages.NEW_MTA_COLOR, mtaColor);

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(execution.getContext());
            ApplicationColorAppender appender = handlerFactory.getApplicationColorAppender(deployedMtaColor, mtaColor);
            descriptor.accept(appender);
            StepsUtil.setUnresolvedDeploymentDescriptor(execution.getContext(), descriptor);

            execution.getContext().setVariable("mtaColor", mtaColor);

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_RENAMING_MODULES);
            throw e;
        }
    }

}
