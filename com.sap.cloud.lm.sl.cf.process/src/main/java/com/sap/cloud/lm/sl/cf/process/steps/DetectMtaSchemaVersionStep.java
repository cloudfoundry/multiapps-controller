package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.MtaSchemaVersionDetector;
import com.sap.cloud.lm.sl.mta.model.SupportedVersions;
import com.sap.cloud.lm.sl.mta.model.Version;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("detectMtaSchemaVersionStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectMtaSchemaVersionStep extends AbstractXS2ProcessStep {

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("detectSchemaVersionTask").displayName("Detect Schema Version").description(
            "Detect Schema Version").build();
    }

    protected Supplier<MtaSchemaVersionDetector> detectorSupplier = () -> new MtaSchemaVersionDetector();

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        getStepLogger().info(Messages.DETECTING_MTA_MAJOR_SCHEMA_VERSION);
        try {
            List<String> extensionDescriptorStrings = StepsUtil.getExtensionDescriptorStrings(context);
            String deploymentDescriptorString = StepsUtil.getDeploymentDescriptorString(context);

            MtaSchemaVersionDetector detector = detectorSupplier.get();
            Version schemaVersion = detector.detect(deploymentDescriptorString, extensionDescriptorStrings);
            if (!SupportedVersions.isSupported(schemaVersion)) {
                throw new SLException(com.sap.cloud.lm.sl.mta.message.Messages.UNSUPPORTED_VERSION, schemaVersion);
            }
            context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, schemaVersion.getMajor());
            context.setVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION, schemaVersion.getMinor());

            getStepLogger().info(Messages.MTA_SCHEMA_VERSION_DETECTED_AS, schemaVersion);

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_DETECTING_MTA_MAJOR_SCHEMA_VERSION);
            throw e;
        }
    }

}
