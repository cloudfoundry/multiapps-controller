package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import java.util.function.Supplier;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.analytics.model.AbstractCommonProcessAttributes;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public abstract class AbstractCommonProcessAttributesCollector<T extends AbstractCommonProcessAttributes> {

    protected abstract T getProcessAttributes();

    public T collectProcessVariables(DelegateExecution execution) {
        T commonProcessVariables = getProcessAttributes();
        commonProcessVariables.setSubscriptionsToDelete(getAttribute(execution, Variables.SUBSCRIPTIONS_TO_DELETE.getName(),
                                                                     () -> VariableHandling.get(execution,
                                                                                                Variables.SUBSCRIPTIONS_TO_DELETE)
                                                                                           .size()));
        commonProcessVariables.setDeletedEntries(getAttribute(execution, Variables.DELETED_ENTRIES.getName(),
                                                              () -> VariableHandling.get(execution, Variables.DELETED_ENTRIES)
                                                                                    .size()));
        commonProcessVariables.setAppsToUndeploy(getAttribute(execution, Variables.APPS_TO_UNDEPLOY.getName(),
                                                              () -> VariableHandling.get(execution, Variables.APPS_TO_UNDEPLOY)
                                                                                    .size()));
        commonProcessVariables.setServicesToDelete(getAttribute(execution, Variables.SERVICES_TO_DELETE.getName(),
                                                                () -> VariableHandling.get(execution, Variables.SERVICES_TO_DELETE)
                                                                                      .size()));
        commonProcessVariables.setUpdatedSubscripers(getAttribute(execution, Variables.UPDATED_SUBSCRIBERS.getName(),
                                                                  () -> VariableHandling.get(execution, Variables.UPDATED_SUBSCRIBERS)
                                                                                        .size()));
        commonProcessVariables.setUpdatedServiceBrokerSubscribers(getAttribute(execution,
                                                                               Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS.getName(),
                                                                               () -> VariableHandling.get(execution,
                                                                                                          Variables.UPDATED_SERVICE_BROKER_SUBSCRIBERS)
                                                                                                     .size()));
        return commonProcessVariables;
    }

    protected <A> A getAttribute(DelegateExecution execution, String variableName, Supplier<A> attributeValueSupplier) {
        return execution.getVariable(variableName) != null ? attributeValueSupplier.get() : null;
    }
}
