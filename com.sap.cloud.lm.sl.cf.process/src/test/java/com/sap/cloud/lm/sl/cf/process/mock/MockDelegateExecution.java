package com.sap.cloud.lm.sl.cf.process.mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.activiti.engine.EngineServices;
import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.persistence.entity.VariableInstance;

public class MockDelegateExecution implements DelegateExecution {

    private Map<String, Object> mockVariables = new HashMap<String, Object>();
    private EngineServices engineServicesMock;

    public static DelegateExecution createSpyInstance() {
        MockDelegateExecution instance = new MockDelegateExecution();
        return spy(instance);
    }

    public Map<String, Object> getMockVariables() {
        return this.mockVariables;
    }

    @Override
    public void createVariableLocal(String arg0, Object arg1) {

    }

    @Override
    public Object getVariable(String arg0) {
        return mockVariables.get(arg0);
    }

    @Override
    public Object getVariableLocal(String arg0) {
        return null;
    }

    @Override
    public Set<String> getVariableNames() {
        return null;
    }

    @Override
    public Set<String> getVariableNamesLocal() {
        return null;
    }

    @Override
    public Map<String, Object> getVariables() {
        return mockVariables;
    }

    @Override
    public Map<String, Object> getVariablesLocal() {
        return null;
    }

    @Override
    public boolean hasVariable(String arg0) {
        return mockVariables.containsKey(arg0);
    }

    @Override
    public boolean hasVariableLocal(String arg0) {
        return false;
    }

    @Override
    public boolean hasVariables() {
        return false;
    }

    @Override
    public boolean hasVariablesLocal() {
        return false;
    }

    @Override
    public void removeVariable(String arg0) {
        this.mockVariables.remove(arg0);
    }

    @Override
    public void removeVariableLocal(String arg0) {
    }

    @Override
    public void removeVariables() {
    }

    @Override
    public void removeVariables(Collection<String> arg0) {
    }

    @Override
    public void removeVariablesLocal() {
    }

    @Override
    public void removeVariablesLocal(Collection<String> arg0) {
    }

    @Override
    public void setVariable(String arg0, Object arg1) {
        this.mockVariables.put(arg0, arg1);

    }

    @Override
    public Object setVariableLocal(String arg0, Object arg1) {
        return null;
    }

    @Override
    public void setVariables(Map<String, ? extends Object> arg0) {
    }

    @Override
    public void setVariablesLocal(Map<String, ? extends Object> arg0) {
    }

    @Override
    public String getBusinessKey() {
        return null;
    }

    @Override
    public String getCurrentActivityId() {
        return "1";
    }

    @Override
    public String getCurrentActivityName() {
        return null;
    }

    @Override
    public EngineServices getEngineServices() {
        if (this.engineServicesMock != null) {
            return this.engineServicesMock;
        }

        this.engineServicesMock = mock(EngineServices.class);
        when(engineServicesMock.getFormService()).thenReturn(mock(FormService.class));
        when(engineServicesMock.getHistoryService()).thenReturn(mock(HistoryService.class));
        when(engineServicesMock.getIdentityService()).thenReturn(mock(IdentityService.class));
        when(engineServicesMock.getManagementService()).thenReturn(mock(ManagementService.class));
        when(engineServicesMock.getRepositoryService()).thenReturn(mock(RepositoryService.class));
        when(engineServicesMock.getRuntimeService()).thenReturn(mock(RuntimeService.class));
        when(engineServicesMock.getTaskService()).thenReturn(mock(TaskService.class));

        return engineServicesMock;
    }

    @Override
    public String getEventName() {
        return null;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getParentId() {
        return null;
    }

    @Override
    public String getProcessBusinessKey() {
        return null;
    }

    @Override
    public String getProcessDefinitionId() {
        return null;
    }

    @Override
    public String getProcessInstanceId() {
        return "1";
    }

    @Override
    public String getTenantId() {
        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getVariables(Collection<String> variableNames) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(Collection<String> variableNames) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getVariables(Collection<String> variableNames, boolean fetchAllVariables) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(Collection<String> variableNames, boolean fetchAllVariables) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getVariablesLocal(Collection<String> variableNames) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(Collection<String> variableNames) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Object> getVariablesLocal(Collection<String> variableNames, boolean fetchAllVariables) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(Collection<String> variableNames, boolean fetchAllVariables) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VariableInstance getVariableInstance(String variableName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getVariable(String variableName, boolean fetchAllVariables) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VariableInstance getVariableInstance(String variableName, boolean fetchAllVariables) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VariableInstance getVariableInstanceLocal(String variableName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getVariableLocal(String variableName, boolean fetchAllVariables) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VariableInstance getVariableInstanceLocal(String variableName, boolean fetchAllVariables) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getVariable(String variableName, Class<T> variableClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> T getVariableLocal(String variableName, Class<T> variableClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setVariable(String variableName, Object value, boolean fetchAllVariables) {
        // TODO Auto-generated method stub

    }

    @Override
    public Object setVariableLocal(String variableName, Object value, boolean fetchAllVariables) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSuperExecutionId() {
        // TODO Auto-generated method stub
        return null;
    }

}
