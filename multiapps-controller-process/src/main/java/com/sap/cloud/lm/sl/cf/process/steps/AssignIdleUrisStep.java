package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("assignIdleUrisStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class AssignIdleUrisStep extends SetAppsUrisStep {

    @Override
    protected String getStartProgressMessage() {
        return Messages.ASSIGNING_IDLE_URIS;
    }

    @Override
    protected String getEndProgressMessage() {
        return Messages.IDLE_URIS_ASSIGNED;
    }

    @Override
    protected List<String> getNewUris(CloudApplicationExtended app) {
        return app.getIdleUris();
    }

    @Override
    protected void setAdditionalContextVariables(DelegateExecution context) {
        StepsUtil.setUseIdleUris(context, true);
    }
}
