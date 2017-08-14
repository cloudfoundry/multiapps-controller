package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;

import org.activiti.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.cts.CtsExtensionsBuilder;
import com.sap.cloud.lm.sl.cts.CtsReturnCode;
import com.sap.cloud.lm.sl.cts.FileInfo;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.slp.Constants;

public class CtsArchiveExtensionsSetter {

    private ProgressMessageService taskExtensionService;
    private Supplier<Date> timestampSupplier;

    public CtsArchiveExtensionsSetter(ProgressMessageService taskExtensionService) {
        this(taskExtensionService, () -> new Date());
    }

    public CtsArchiveExtensionsSetter(ProgressMessageService taskExtensionService, Supplier<Date> timestampSupplier) {
        this.taskExtensionService = taskExtensionService;
        this.timestampSupplier = timestampSupplier;
    }

    public void set(DelegateExecution context) throws SLException {
        String logsPath = com.sap.cloud.lm.sl.cf.core.Constants.CTS_LOGS_ENDPOINT + StepsUtil.getCtsCurrentFileInfo(context).getFileName();
        String mtaDeployActivityId = (String) context.getVariable(Constants.INDEXED_STEP_NAME);
        CtsReturnCode ctsErrorCode = StepsUtil.getCtsReturnCode(context);

        FileInfo fileInfo = StepsUtil.getCtsCurrentFileInfo(context);

        CtsExtensionsBuilder extensionsBuilder = new CtsExtensionsBuilder();
        extensionsBuilder.ctsErrorCode(ctsErrorCode);
        extensionsBuilder.fileInfo(fileInfo);
        extensionsBuilder.logsPath(logsPath);
        Map<String, Object> extensions = extensionsBuilder.build();

        ProgressMessageType progressMessageType = ProgressMessageType.EXT;
        String progressMessageText = JsonUtil.toJson(extensions);
// @formatter:off
        taskExtensionService.add(new ProgressMessage(
            StepsUtil.getCorrelationId(context),
            mtaDeployActivityId,
            progressMessageType,
            progressMessageText,
            timestampSupplier.get())
        );
// @formatter:on
    }
}
