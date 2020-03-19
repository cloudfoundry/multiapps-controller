package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import java.util.function.Supplier;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AbstractCommonProcessAttributes;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;

public abstract class AbstractCommonProcessAttributesCollector<T extends AbstractCommonProcessAttributes> {

    protected abstract T getProcessAttributes();

    // @formatter:off
    public T collectProcessVariables(DelegateExecution execution) {
        T commonProcessVariables = getProcessAttributes();
        commonProcessVariables.setSubscriptionsToDelete(
            getAttribute(execution, Constants.VAR_SUBSCRIPTIONS_TO_DELETE, () -> VariableHandling.get(execution, Variables.SUBSCRIPTIONS_TO_DELETE).size()));
        commonProcessVariables.setDeletedEntries(
            getAttribute(execution, Constants.VAR_DELETED_ENTRIES, () -> VariableHandling.get(execution, Variables.DELETED_ENTRIES).size()));
        commonProcessVariables.setAppsToUndeploy(
            getAttribute(execution, Constants.VAR_APPS_TO_UNDEPLOY, () -> StepsUtil.getAppsToUndeploy(execution).size()));
        commonProcessVariables.setServicesToDelete(
            getAttribute(execution, Constants.VAR_SERVICES_TO_DELETE, () -> VariableHandling.get(execution, Variables.SERVICES_TO_DELETE).size()));
        commonProcessVariables.setUpdatedSubscripers(
            getAttribute(execution, Constants.VAR_UPDATED_SUBSCRIBERS, () -> VariableHandling.get(execution, Variables.UPDATED_SUBSCRIBERS).size()));
        commonProcessVariables.setUpdatedServiceBrokerSubscribers(
            getAttribute(execution, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS, () -> VariableHandling.get(execution, Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS).size()));

        return commonProcessVariables;
    }
 // @formatter:on

    protected <A> A getAttribute(DelegateExecution execution, String variableName, Supplier<A> attributeValueSupplier) {
        return execution.getVariable(variableName) != null ? attributeValueSupplier.get() : null;
    }
}
