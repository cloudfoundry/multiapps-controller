package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.function.Supplier;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.SchemaVersionDetector;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.SupportedVersions;
import com.sap.cloud.lm.sl.mta.model.Version;

@Component("detectMtaSchemaVersionStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectMtaSchemaVersionStep extends SyncFlowableStep {

    protected Supplier<SchemaVersionDetector> detectorSupplier = SchemaVersionDetector::new;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DETECTING_MTA_MAJOR_SCHEMA_VERSION);
        DeploymentDescriptor deploymentDescriptor = StepsUtil.getDeploymentDescriptor(execution.getContext());
        List<ExtensionDescriptor> extensionDescriptors = StepsUtil.getExtensionDescriptorChain(execution.getContext());

        SchemaVersionDetector detector = detectorSupplier.get();
        Version schemaVersion = detector.detect(deploymentDescriptor, extensionDescriptors);
        if (!SupportedVersions.isSupported(schemaVersion)) {
            throw new SLException(com.sap.cloud.lm.sl.mta.message.Messages.UNSUPPORTED_VERSION, schemaVersion);
        }
        if (!SupportedVersions.isFullySupported(schemaVersion)) {
            getStepLogger().warn(Messages.UNSUPPORTED_MINOR_VERSION, schemaVersion);
        }
        execution.getContext()
            .setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, schemaVersion.getMajor());

        getStepLogger().info(Messages.MTA_SCHEMA_VERSION_DETECTED_AS, schemaVersion.getMajor());

        return StepPhase.DONE;
    }

    @Override
    protected void onStepError(DelegateExecution context, Exception e) throws Exception {
        getStepLogger().error(e, Messages.ERROR_DETECTING_MTA_MAJOR_SCHEMA_VERSION);
        throw e;
    }

}
