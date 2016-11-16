package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class DetectMtaSchemaVersionStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(DetectMtaSchemaVersionStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("detectSchemaVersionTask", "Detect Schema Version", "Detect Schema Version");
    }

    protected Supplier<MtaSchemaVersionDetector> detectorSupplier = () -> new MtaSchemaVersionDetector();

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        info(context, Messages.DETECTING_MTA_MAJOR_SCHEMA_VERSION, LOGGER);
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

            info(context, format(Messages.MTA_SCHEMA_VERSION_DETECTED_AS, schemaVersion), LOGGER);

            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_DETECTING_MTA_MAJOR_SCHEMA_VERSION, e, LOGGER);
            throw e;
        }
    }
}
