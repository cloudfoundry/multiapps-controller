package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
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
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BlueGreenRenameStep extends AbstractXS2ProcessStep {

    private static final ApplicationColor DEFAULT_MTA_COLOR = ApplicationColor.BLUE;

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("blueGreenRenameTask").displayName("Blue Green Rename").description("Blue Green Rename").build();
    }

    protected Supplier<ApplicationColorDetector> colorDetectorSupplier = () -> new ApplicationColorDetector();

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        try {
            getStepLogger().info(Messages.DETECTING_COLOR_OF_DEPLOYED_MTA);

            DeploymentDescriptor descriptor = StepsUtil.getUnresolvedDeploymentDescriptor(context);
            DeployedMta deployedMta = StepsUtil.getDeployedMta(context);

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
            getStepLogger().info(Messages.NEW_MTA_COLOR, mtaColor);

            HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context);
            ApplicationColorAppender appender = handlerFactory.getApplicationColorAppender(deployedMtaColor, mtaColor);
            descriptor.accept(appender);
            StepsUtil.setUnresolvedDeploymentDescriptor(context, descriptor);

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_RENAMING_MODULES);
            throw e;
        }
    }

}
