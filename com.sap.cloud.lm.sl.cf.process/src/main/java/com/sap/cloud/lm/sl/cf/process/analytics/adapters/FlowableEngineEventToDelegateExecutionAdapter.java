package com.sap.cloud.lm.sl.cf.process.analytics.adapters;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.context.Context;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;

public class FlowableEngineEventToDelegateExecutionAdapter implements DelegateExecution {

    private FlowableEngineEvent event;

    public FlowableEngineEventToDelegateExecutionAdapter(FlowableEngineEvent event) {
        this.event = event;
    }

    @Override
    public Object getVariable(String variableName) {
        HistoricVariableInstance result = Context.getProcessEngineConfiguration()
            .getHistoryService()
            .createHistoricVariableInstanceQuery()
            .processInstanceId(getProcessInstanceId())
            .variableName(variableName)
            .singleResult();
        if (result != null) {
            return result.getValue();
        }
        return result;
    }

    @Override
    public String getProcessInstanceId() {
        return event.getProcessInstanceId();
    }

    @Override
    public <T> T getVariable(String arg0, Class<T> arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstance getVariableInstance(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstance getVariableInstance(String arg0, boolean arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstance getVariableInstanceLocal(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstance getVariableInstanceLocal(String arg0, boolean arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(Collection<String> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(Collection<String> arg0, boolean arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(Collection<String> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(Collection<String> arg0, boolean arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getVariableLocal(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getVariableLocal(String arg0, boolean arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T getVariableLocal(String arg0, Class<T> arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getVariableNames() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> getVariableNamesLocal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getVariables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getVariables(Collection<String> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getVariables(Collection<String> arg0, boolean arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getVariablesLocal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getVariablesLocal(Collection<String> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getVariablesLocal(Collection<String> arg0, boolean arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasVariable(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasVariableLocal(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasVariables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasVariablesLocal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVariable(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVariableLocal(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVariables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVariables(Collection<String> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVariablesLocal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeVariablesLocal(Collection<String> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVariable(String arg0, Object arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVariable(String arg0, Object arg1, boolean arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object setVariableLocal(String arg0, Object arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object setVariableLocal(String arg0, Object arg1, boolean arg2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVariables(Map<String, ? extends Object> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVariablesLocal(Map<String, ? extends Object> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCurrentActivityId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getEventName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getParentId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProcessDefinitionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSuperExecutionId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getTenantId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getVariable(String arg0, boolean arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getTransientVariable(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getTransientVariableLocal(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getTransientVariables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getTransientVariablesLocal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTransientVariable(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTransientVariableLocal(String arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTransientVariables() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTransientVariablesLocal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTransientVariable(String arg0, Object arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTransientVariableLocal(String arg0, Object arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTransientVariables(Map<String, Object> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTransientVariablesLocal(Map<String, Object> arg0) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRootProcessInstanceId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEventName(String eventName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProcessInstanceBusinessKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowElement getCurrentFlowElement() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCurrentFlowElement(FlowElement flowElement) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowableListener getCurrentFlowableListener() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setCurrentFlowableListener(FlowableListener currentListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DelegateExecution getParent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends DelegateExecution> getExecutions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setActive(boolean isActive) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isActive() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEnded() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setConcurrent(boolean isConcurrent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConcurrent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProcessInstanceType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void inactivate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isScope() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setScope(boolean isScope) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMultiInstanceRoot() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMultiInstanceRoot(boolean isMultiInstanceRoot) {
        throw new UnsupportedOperationException();
    }
}