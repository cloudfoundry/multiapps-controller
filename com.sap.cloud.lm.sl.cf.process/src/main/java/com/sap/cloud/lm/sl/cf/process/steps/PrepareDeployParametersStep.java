package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.cts.FileInfo;
import com.sap.cloud.lm.sl.cts.FileInfoFactory;
import com.sap.cloud.lm.sl.slp.model.LoopStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("prepareDeployParametersStep")
public class PrepareDeployParametersStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareDeployParametersStep.class);

    public static StepMetadata getMetadata() {
        return LoopStepMetadata.builder().id(getId()).displayName("Prepare Deploy Parameters").description(
            "Prepare Deploy Parameters").children(StartMtaDeploySubProcessStep.getMetadata(),
                SetCtsArchiveExtensionsStep.getMetadata()).countVariable(Constants.VAR_MTARS_COUNT).build();
    }

    public static String getId() {
        return "prepareDeployParametersTask";
    }

    protected Supplier<FileInfoFactory> fileInfoFactorySupplier = () -> new FileInfoFactory();

    @Override
    public ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);
        info(context, Messages.PREPARING_DEPLOY_PARAMETERS, LOGGER);
        try {
            List<FileInfo> fileInfoList = getFileInfoList(context);
            initializeCtsLoggers(context, fileInfoList);
            context.setVariable(Constants.VAR_MTARS_COUNT, fileInfoList.size());
            context.setVariable(Constants.VAR_MTARS_INDEX, 0);
            context.setVariable(Constants.VAR_INDEX_VARIABLE_NAME, Constants.VAR_MTARS_INDEX);

            StepsUtil.setCtsFileInfoList(context, fileInfoList);

        } catch (SLException e) {
            error(context, Messages.ERROR_PREPARING_DEPLOY_PARAMETERS, e, LOGGER);
            throw e;
        }
        debug(context, Messages.DEPLOY_PARAMETERS_PREPARED, LOGGER);
        return ExecutionStatus.SUCCESS;
    }

    private void initializeCtsLoggers(DelegateExecution context, List<FileInfo> fileInfoList) {
        for (FileInfo fileInfo : fileInfoList) {
            StepsUtil.initSpecificCtsLog(context, processLoggerProviderFactory, fileInfo.getFileName());
        }
    }

    private List<FileInfo> getFileInfoList(DelegateExecution context) throws SLException {
        @SuppressWarnings("unchecked")
        List<Map<String, Map<String, String>>> fileInfoListParameter = (List<Map<String, Map<String, String>>>) context.getVariable(
            Constants.PARAM_FILE_LIST);
        if (fileInfoListParameter == null || fileInfoListParameter.isEmpty()) {
            throw new SLException(Messages.REQUIRED_PARAMETER_IS_MISSING, Constants.PARAM_FILE_LIST);
        }
        return fileInfoFactorySupplier.get().fromSlpTable(fileInfoListParameter);
    }

}
