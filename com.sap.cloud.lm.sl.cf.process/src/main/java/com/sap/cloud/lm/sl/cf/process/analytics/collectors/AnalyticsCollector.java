package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricProcessInstance;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AbstractCommonProcessAttributes;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AnalyticsData;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

@Named("analyticsCollector")
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

    LongSupplier endTimeSupplier = System::currentTimeMillis;
    Supplier<ZoneId> timeZoneSupplier = ZoneId::systemDefault;

    public AnalyticsData collectAnalyticsData(DelegateExecution execution) {
        String processId = execution.getProcessInstanceId();
        ProcessType processType = processTypeParser.getProcessType(execution);
        long startTime = getStartTime(processId);
        long endTime = getEndTime();
        long processDuration = getProcessDurationInSeconds(processId);
        String mtaId = VariableHandling.get(execution, Variables.MTA_ID);
        String org = VariableHandling.get(execution, Variables.ORG);
        String space = VariableHandling.get(execution, Variables.SPACE);
        String controllerUrl = configuration.getControllerUrl()
                                            .toString();
        AbstractCommonProcessAttributes attributes = getProcessType(processType).collectProcessVariables(execution);

        return new AnalyticsData(processId,
                                 processType,
                                 startTime,
                                 endTime,
                                 processDuration,
                                 null,
                                 mtaId,
                                 org,
                                 space,
                                 controllerUrl,
                                 attributes);

    }

    public AbstractCommonProcessAttributesCollector getProcessType(ProcessType processType) {
        if (processType.equals(ProcessType.BLUE_GREEN_DEPLOY) || processType.equals(ProcessType.DEPLOY)) {
            return deployProcessAttributesCollector;
        }
        if (processType.equals(ProcessType.UNDEPLOY)) {
            return undeployProcessAttributesCollector;
        }
        return null;
    }

    public long getStartTime(String processId) {
        HistoryService historyService = processEngineConfiguration.getHistoryService();
        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                                                                .processInstanceId(processId)
                                                                .singleResult();
        return processInstance.getStartTime()
                              .getTime();
    }

    protected long getEndTime() {
        Date date = new Date(endTimeSupplier.getAsLong());
        return date.getTime();
    }

    public long getProcessDurationInSeconds(String processId) {
        long startTime = getStartTime(processId);
        long endTime = getEndTime();
        return TimeUnit.MILLISECONDS.toSeconds(endTime - startTime);
    }

}
