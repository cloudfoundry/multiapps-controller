package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.CtsArchiveExtensionsSetter;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.cts.CtsReturnCode;
import com.sap.cloud.lm.sl.cts.FileInfo;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("setCtsArchiveExtensionsStep")
public class SetCtsArchiveExtensionsStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetCtsArchiveExtensionsStep.class);

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("setCtsArchiveExtensionsTask").displayName("Set CTS+ Archive Status").description(
            "Set CTS+ Archive Status").build();
    }

    protected Supplier<CtsArchiveExtensionsSetter> extensionsSetterSupplier = () -> new CtsArchiveExtensionsSetter(taskExtensionService);

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        FileInfo fileInfo = StepsUtil.getCtsCurrentFileInfo(context);
        String fileName = fileInfo.getFileName();
        String subProcessId = StepsUtil.getSubProcessId(context);
        updateCtsReturnCodeFromSubProcess(context, subProcessId);
        try {
            info(context, format(Messages.SETTING_CTS_ARCHIVE_EXTENSIONS, fileName), LOGGER);
            extensionsSetterSupplier.get().set(context);
            debug(context, Messages.CTS_ARCHIVE_EXTENSIONS_SET, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, format(Messages.ERROR_SETTING_CTS_ARCHIVE_STATUS, fileName), e, LOGGER);
            throw e;
        }
    }

    private void updateCtsReturnCodeFromSubProcess(DelegateExecution context, String subProcessId) {
        CtsReturnCode ctsReturnCode = getCtsReturnCodeFromSubProcess(context, subProcessId);
        StepsUtil.setCtsReturnCode(context, ctsReturnCode);
    }

    protected CtsReturnCode getCtsReturnCodeFromSubProcess(DelegateExecution context, String subProcessId) {
        HistoricVariableInstance ctsReturnCode = context.getEngineServices().getHistoryService().createHistoricVariableInstanceQuery().processInstanceId(
            subProcessId).variableName(Constants.VAR_CTS_RETURN_CODE).singleResult();
        if (ctsReturnCode == null) {
            return CtsReturnCode.OK;
        }
        return CtsReturnCode.valueOf((String) ctsReturnCode.getValue());
    }

}
