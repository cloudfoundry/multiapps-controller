package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import java.util.function.Supplier;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AbstractCommonProcessAttributes;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;

public abstract class AbstractCommonProcessAttributesCollector<T extends AbstractCommonProcessAttributes> {

    protected abstract T getProcessAttributes();

    // @formatter:off
    public T collectProcessVariables(DelegateExecution context) {
        T commonProcessVariables = getProcessAttributes();
        commonProcessVariables.setSubscriptionsToDelete(
            getAttribute(context, Constants.VAR_SUBSCRIPTIONS_TO_DELETE, () -> StepsUtil.getSubscriptionsToDelete(context).size()));
        commonProcessVariables.setDeletedEntries(
            getAttribute(context, Constants.VAR_DELETED_ENTRIES, () -> StepsUtil.getDeletedEntries(context).size()));
        commonProcessVariables.setAppsToUndeploy(
            getAttribute(context, Constants.VAR_APPS_TO_UNDEPLOY, () -> StepsUtil.getAppsToUndeploy(context).size()));
        commonProcessVariables.setServicesToDelete(
            getAttribute(context, Constants.VAR_SERVICES_TO_DELETE, () -> StepsUtil.getServicesToDelete(context).size()));
        commonProcessVariables.setUpdatedSubscripers(
            getAttribute(context, Constants.VAR_UPDATED_SUBSCRIBERS, () -> StepsUtil.getUpdatedSubscribers(context).size()));
        commonProcessVariables.setUpdatedServiceBrokerSubscribers(
            getAttribute(context, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS, () -> StepsUtil.getServiceBrokerSubscribersToRestart(context).size()));

        return commonProcessVariables;
    }
 // @formatter:on

    private <A> A getAttribute(DelegateExecution context, String variableName, Supplier<A> attributeValueSupplier) {
        return context.getVariable(variableName) != null ? attributeValueSupplier.get() : null;
    }
}
