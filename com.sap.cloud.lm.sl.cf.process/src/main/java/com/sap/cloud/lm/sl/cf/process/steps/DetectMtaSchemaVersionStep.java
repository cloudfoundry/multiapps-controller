package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Named;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.SchemaVersionDetector;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.SupportedVersions;
import com.sap.cloud.lm.sl.mta.model.Version;

@Named("detectMtaSchemaVersionStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectMtaSchemaVersionStep extends SyncFlowableStep {

    protected Supplier<SchemaVersionDetector> detectorSupplier = SchemaVersionDetector::new;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DETECTING_MTA_MAJOR_SCHEMA_VERSION);
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        List<ExtensionDescriptor> extensionDescriptors = StepsUtil.getExtensionDescriptorChain(context.getExecution());

        SchemaVersionDetector detector = detectorSupplier.get();
        Version schemaVersion = detector.detect(deploymentDescriptor, extensionDescriptors);
        if (!SupportedVersions.isSupported(schemaVersion)) {
            throw new SLException(com.sap.cloud.lm.sl.mta.Messages.UNSUPPORTED_VERSION, schemaVersion);
        }
        if (!SupportedVersions.isFullySupported(schemaVersion)) {
            getStepLogger().warn(Messages.UNSUPPORTED_MINOR_VERSION, schemaVersion);
        }
        context.getExecution()
               .setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, schemaVersion.getMajor());

        getStepLogger().info(Messages.MTA_SCHEMA_VERSION_DETECTED_AS, schemaVersion.getMajor());

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETECTING_MTA_MAJOR_SCHEMA_VERSION;
    }

}
