package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cts.FileInfo;
import com.sap.cloud.lm.sl.slp.model.AsyncStepMetadata;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("startMtaDeploySubProcessStep")
public class StartMtaDeploySubProcessStep extends AbstractXS2SubProcessStarterStep {

    public static StepMetadata getMetadata() {
        return AsyncStepMetadata.builder().id("startDeploySubProcessTask").displayName("Start Deploy SubProcess").description(
            "Start Deploy SubProcess").pollTaskId("monitorDeployProcessTask").build();
    }

    @Override
    protected String getIterationVariableName() {
        return Constants.PARAM_APP_ARCHIVE_ID;
    }

    @Override
    protected String getProcessDefinitionKey() {
        return Constants.DEPLOY_SERVICE_ID;
    }

    @Override
    protected Object getIterationVariable(DelegateExecution context, int index) {
        List<FileInfo> fileInfoList = StepsUtil.getCtsFileInfoList(context);
        FileInfo currentFileInfo = fileInfoList.get(index);
        StepsUtil.setCtsCurrentFileInfo(context, currentFileInfo);
        return currentFileInfo.getFileId();
    }

    @Override
    public String getLogicalStepName() {
        return StartMtaDeploySubProcessStep.class.getSimpleName();
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_MTARS_INDEX;
    }

}