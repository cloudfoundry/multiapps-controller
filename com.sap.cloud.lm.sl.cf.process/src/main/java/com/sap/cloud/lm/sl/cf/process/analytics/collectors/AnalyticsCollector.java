package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AbstractCommonProcessAttributes;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AnalyticsData;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

@Component("analyticsCollector")
public class AnalyticsCollector {

    @Inject
    private ApplicationConfiguration configuration;

    @Inject
    public DeployProcessAttributesCollector deployProcessAttributesCollector;

    @Inject
    public UndeployProcessAttributesCollector undeployProcessAttributesCollector;

    @Inject
    private ProcessTypeParser processTypeParser;

    @Inject
    private ProcessEngineConfiguration processEngineConfiguration;

    Supplier<Long> endTimeSupplier = System::currentTimeMillis;
    Supplier<ZoneId> timeZoneSupplier = ZoneId::systemDefault;

    public AnalyticsData collectAnalyticsData(DelegateExecution context) {
        String processId = context.getProcessInstanceId();
        ProcessType processType = processTypeParser.getProcessType(context);
        long startTime = getStartTime(context, processId);
        long endTime = getEndTime();
        long processDuration = getProcessDurationInSeconds(context, processId);
        String mtaId = (String) context.getVariable(Constants.PARAM_MTA_ID);
        String platform = configuration.getPlatformType()
            .toString();
        String org = StepsUtil.getOrg(context);
        String space = StepsUtil.getSpace(context);
        String controllerUrl = configuration.getControllerUrl()
            .toString();
        AbstractCommonProcessAttributes attributes = getProcessType(processType).collectProcessVariables(context);

        return new AnalyticsData(processId, processType, startTime, endTime, processDuration, null, mtaId, platform, org, space, controllerUrl,
            attributes);

    }

    public AbstractCommonProcessAttributesCollector<?> getProcessType(ProcessType processType) {
        if (processType.equals(ProcessType.BLUE_GREEN_DEPLOY) || processType.equals(ProcessType.DEPLOY)) {
            return deployProcessAttributesCollector;
        }
        if (processType.equals(ProcessType.UNDEPLOY)) {
            return undeployProcessAttributesCollector;
        }
        return null;
    }

    public long getStartTime(DelegateExecution context, String processId) {
        HistoryService historyService = processEngineConfiguration.getHistoryService();
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
            .processInstanceId(processId)
            .singleResult();
        return processInstance.getStartTime()
            .getTime();
    }

    protected long getEndTime() {
        Date date = new Date(endTimeSupplier.get());
        return date.getTime();
    }

    public long getProcessDurationInSeconds(DelegateExecution context, String processId) {
        long startTime = getStartTime(context, processId);
        long endTime = getEndTime();
        return TimeUnit.MILLISECONDS.toSeconds(endTime - startTime);
    }

}
