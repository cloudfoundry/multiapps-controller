package com.sap.cloud.lm.sl.cf.process.util;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.getOrDefault;
import static java.text.MessageFormat.format;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.activiti.engine.HistoryService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.history.HistoricActivityInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.PrepareDeployParametersStep;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.cts.CtsExtensionsBuilder;
import com.sap.cloud.lm.sl.cts.CtsReturnCode;
import com.sap.cloud.lm.sl.cts.FileInfo;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

public class CtsArchiveExtensionsSetter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CtsArchiveExtensionsSetter.class);

    private ProgressMessageService taskExtensionService;
    private Supplier<Date> timestampSupplier;

    public CtsArchiveExtensionsSetter(ProgressMessageService taskExtensionService) {
        this(taskExtensionService, null);
    }

    public CtsArchiveExtensionsSetter(ProgressMessageService taskExtensionService, Supplier<Date> timestampSupplier) {
        this.taskExtensionService = taskExtensionService;
        this.timestampSupplier = timestampSupplier;
    }

    public void set(DelegateExecution context) throws SLException {
        String logsPath = com.sap.cloud.lm.sl.cf.core.Constants.CTS_LOGS_ENDPOINT + StepsUtil.CTS_LOG_NAME;
        String mtaDeployActivityId = PrepareDeployParametersStep.getId();
        CtsReturnCode ctsErrorCode = StepsUtil.getCtsReturnCode(context);

        FileInfo fileInfo = StepsUtil.getCtsCurrentFileInfo(context);

        CtsExtensionsBuilder extensionsBuilder = new CtsExtensionsBuilder();
        extensionsBuilder.ctsErrorCode(ctsErrorCode);
        extensionsBuilder.fileInfo(fileInfo);
        extensionsBuilder.logsPath(logsPath);
        Map<String, Object> extensions = extensionsBuilder.build();

        // Get the last 'PrepareDeployParametersStep' activity. There can be more than one if
        // multiple MTAs are being deployed:
        Date currentMtaDeployStartTime = getOrDefault(timestampSupplier, () -> {
            return getMostRecentActivity(context, mtaDeployActivityId).getStartTime();
        }).get();
        LOGGER.debug(format(Messages.ARCHIVE_DEPLOY_ACTIVITY_START_TIME, fileInfo.getFileName(), currentMtaDeployStartTime));

        ProgressMessageType progressMessageType = ProgressMessageType.EXT;
        String progressMessageText = JsonUtil.toJson(extensions);
// @formatter:off
        taskExtensionService.add(new ProgressMessage(
            context.getProcessInstanceId(),
            mtaDeployActivityId,
            progressMessageType,
            progressMessageText,
            currentMtaDeployStartTime)
        );
// @formatter:on
    }

    private HistoricActivityInstance getMostRecentActivity(DelegateExecution context, String activityId) {
        HistoryService historyService = context.getEngineServices().getHistoryService();
        String processId = context.getProcessInstanceId();
// @formatter:off
        List<HistoricActivityInstance> activities =  historyService.createHistoricActivityInstanceQuery( )
            .processInstanceId(processId)
            .activityId(activityId)
            .orderByHistoricActivityInstanceEndTime()
            .desc()
            .list();
// @formatter:on
        return activities.stream().findFirst().orElseThrow(
            () -> new IllegalStateException(Messages.COULD_NOT_FIND_LAST_MTA_DEPLOY_ACTIVITY));
    }

}
