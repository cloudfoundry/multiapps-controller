package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.function.Supplier;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.SchemaVersionDetector;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.SupportedVersions;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("detectMtaSchemaVersionStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetectMtaSchemaVersionStep extends SyncFlowableStep {

    protected Supplier<SchemaVersionDetector> detectorSupplier = SchemaVersionDetector::new;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.DETECTING_MTA_MAJOR_SCHEMA_VERSION);
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        List<ExtensionDescriptor> extensionDescriptors = context.getVariable(Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN);

        SchemaVersionDetector detector = detectorSupplier.get();
        Version schemaVersion = detector.detect(deploymentDescriptor, extensionDescriptors);
        if (!SupportedVersions.isSupported(schemaVersion)) {
            throw new SLException(org.cloudfoundry.multiapps.mta.Messages.UNSUPPORTED_VERSION, schemaVersion);
        }
        if (!SupportedVersions.isFullySupported(schemaVersion)) {
            getStepLogger().warn(Messages.UNSUPPORTED_MINOR_VERSION, schemaVersion);
        }
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, schemaVersion.getMajor());

        getStepLogger().info(Messages.MTA_SCHEMA_VERSION_DETECTED_AS, schemaVersion.getMajor());

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DETECTING_MTA_MAJOR_SCHEMA_VERSION;
    }

}
