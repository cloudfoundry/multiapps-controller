package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.clients.RecentLogsRetriever;

@Component("pollReExecuteAppStatusStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollReExecuteAppStatusStep extends PollExecuteAppStatusStep {

    public PollReExecuteAppStatusStep(RecentLogsRetriever recentLogsRetriever) {
        super(recentLogsRetriever);
    }

    @Override
    protected CloudApplication getNextApp(DelegateExecution context) {
        return StepsUtil.getAppToRestart(context);
    }

}
