package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.CtsProcessExtensionsSetter;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("setCtsProcessExtensionsStep")
public class SetCtsProcessExtensionsStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetCtsProcessExtensionsStep.class);

    public static StepMetadata getMetadata() {
        return new StepMetadata("setCtsProcessStatusTask", "Set CTS+ Process Status", "Set CTS+ Process Status");
    }

    protected Supplier<CtsProcessExtensionsSetter> extensionsSetterSupplier = () -> new CtsProcessExtensionsSetter(taskExtensionService);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        try {
            info(context, Messages.SETTING_CTS_PROCESS_EXTENSIONS, LOGGER);
            extensionsSetterSupplier.get().set(context);
            debug(context, Messages.CTS_PROCESS_EXTENSIONS_SET, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_SETTING_CTS_PROCESS_STATUS, e, LOGGER);
            throw e;
        }
    }

}
