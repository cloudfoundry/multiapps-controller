package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("blueGreenRenameStep")
public class BlueGreenRenameStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueGreenRenameStep.class);

    private static final ApplicationColor DEFAULT_MTA_COLOR = ApplicationColor.BLUE;

    public static StepMetadata getMetadata() {
        return new StepMetadata("blueGreenRenameTask", "Blue Green Rename", "Blue Green Rename");
    }

    protected Supplier<ApplicationColorDetector> colorDetectorSupplier = () -> new ApplicationColorDetector();

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        try {
            info(context, Messages.DETECTING_COLOR_OF_DEPLOYED_MTA, LOGGER);

            DeploymentDescriptor descriptor = StepsUtil.getDeploymentDescriptor(context);
            DeployedMta deployedMta = StepsUtil.getDeployedMta(context);

            ApplicationColorDetector detector = colorDetectorSupplier.get();
            ApplicationColor mtaColor;
            try {
                ApplicationColor deployedMtaColor = detector.detectSingularDeployedApplicationColor(deployedMta);
                if (deployedMtaColor != null) {
                    info(context, format(Messages.DEPLOYED_MTA_COLOR, deployedMtaColor), LOGGER);
                    mtaColor = deployedMtaColor.getAlternativeColor();
                } else {
                    mtaColor = DEFAULT_MTA_COLOR;
                }
            } catch (ConflictException e) {
                warn(context, e.getMessage(), LOGGER);
                // Assume that the last deployed color was not deployed successfully and try to update (fix) it in this process:
                ApplicationColor liveMtaColor = detector.detectFirstDeployedApplicationColor(deployedMta);
                ApplicationColor idleMtaColor = liveMtaColor.getAlternativeColor();
                info(context, format(Messages.ASSUMED_LIVE_AND_IDLE_COLORS, liveMtaColor, idleMtaColor), LOGGER);
                mtaColor = idleMtaColor;
            }
            info(context, format(Messages.NEW_MTA_COLOR, mtaColor), LOGGER);

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
            ApplicationColorAppender appender = handlerFactory.getApplicationColorAppender(mtaColor);
            descriptor.accept(appender);
            StepsUtil.setDeploymentDescriptor(context, descriptor);

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_RENAMING_MODULES, e, LOGGER);
            throw e;
        }
    }

}
