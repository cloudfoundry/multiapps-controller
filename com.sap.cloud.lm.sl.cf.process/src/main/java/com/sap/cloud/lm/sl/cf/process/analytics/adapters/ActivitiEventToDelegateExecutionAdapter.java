package com.sap.cloud.lm.sl.cf.process.analytics.adapters;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.EngineServices;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.persistence.entity.VariableInstance;

public class ActivitiEventToDelegateExecutionAdapter implements DelegateExecution {

    private ActivitiEvent event;

    public ActivitiEventToDelegateExecutionAdapter(ActivitiEvent event) {
        this.event = event;
    }

    @Override
    public Object getVariable(String variableName) {
        HistoricVariableInstance result = event.getEngineServices()
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
    public EngineServices getEngineServices() {
        return event.getEngineServices();
    }

    @Override
    public void createVariableLocal(String arg0, Object arg1) {
        throw new UnsupportedOperationException();
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
    public String getBusinessKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCurrentActivityId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCurrentActivityName() {
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
    public String getProcessBusinessKey() {
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
}