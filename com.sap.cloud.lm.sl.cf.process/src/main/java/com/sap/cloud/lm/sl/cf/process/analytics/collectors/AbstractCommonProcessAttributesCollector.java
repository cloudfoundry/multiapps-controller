package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import java.util.function.Supplier;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AbstractCommonProcessAttributes;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.process.variables.VariablesHandler;

public abstract class AbstractCommonProcessAttributesCollector<T extends AbstractCommonProcessAttributes> {

    protected abstract T getProcessAttributes();

    // @formatter:off
    public T collectProcessVariables(DelegateExecution execution) {
        VariablesHandler variablesHandler = new VariablesHandler(execution);
        T commonProcessVariables = getProcessAttributes();
        commonProcessVariables.setSubscriptionsToDelete(
            getAttribute(execution, Constants.VAR_SUBSCRIPTIONS_TO_DELETE, () -> variablesHandler.get(Variables.SUBSCRIPTIONS_TO_DELETE).size()));
        commonProcessVariables.setDeletedEntries(
            getAttribute(execution, Constants.VAR_DELETED_ENTRIES, () -> variablesHandler.get(Variables.DELETED_ENTRIES).size()));
        commonProcessVariables.setAppsToUndeploy(
            getAttribute(execution, Constants.VAR_APPS_TO_UNDEPLOY, () -> StepsUtil.getAppsToUndeploy(execution).size()));
        commonProcessVariables.setServicesToDelete(
            getAttribute(execution, Constants.VAR_SERVICES_TO_DELETE, () -> variablesHandler.get(Variables.SERVICES_TO_DELETE).size()));
        commonProcessVariables.setUpdatedSubscripers(
            getAttribute(execution, Constants.VAR_UPDATED_SUBSCRIBERS, () -> variablesHandler.get(Variables.UPDATED_SUBSCRIBERS).size()));
        commonProcessVariables.setUpdatedServiceBrokerSubscribers(
            getAttribute(execution, Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS, () -> variablesHandler.get(Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS).size()));

        return commonProcessVariables;
    }
 // @formatter:on

    protected <A> A getAttribute(DelegateExecution execution, String variableName, Supplier<A> attributeValueSupplier) {
        return execution.getVariable(variableName) != null ? attributeValueSupplier.get() : null;
    }
}
