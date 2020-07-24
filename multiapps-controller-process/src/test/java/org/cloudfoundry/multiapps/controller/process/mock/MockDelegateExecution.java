package org.cloudfoundry.multiapps.controller.process.mock;

import static org.mockito.Mockito.spy;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.mockito.Mockito;

public class MockDelegateExecution implements DelegateExecution {

    private final Map<String, Object> mockVariables = new HashMap<>();

    public static DelegateExecution createSpyInstance() {
        MockDelegateExecution instance = new MockDelegateExecution();
        return spy(instance);
    }

    public Map<String, Object> getMockVariables() {
        return this.mockVariables;
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
    public void setVariables(Map<String, ?> arg0) {
    }

    @Override
    public void setVariablesLocal(Map<String, ?> arg0) {
    }

    @Override
    public String getCurrentActivityId() {
        return "1";
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
    public String getProcessDefinitionId() {
        return null;
    }

    @Override
    public String getPropagatedStageInstanceId() {
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

        return null;
    }

    @Override
    public Map<String, Object> getVariables(Collection<String> variableNames) {

        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(Collection<String> variableNames) {

        return null;
    }

    @Override
    public Map<String, Object> getVariables(Collection<String> variableNames, boolean fetchAllVariables) {

        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(Collection<String> variableNames, boolean fetchAllVariables) {

        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal() {

        return null;
    }

    @Override
    public Map<String, Object> getVariablesLocal(Collection<String> variableNames) {

        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(Collection<String> variableNames) {

        return null;
    }

    @Override
    public Map<String, Object> getVariablesLocal(Collection<String> variableNames, boolean fetchAllVariables) {

        return null;
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(Collection<String> variableNames, boolean fetchAllVariables) {

        return null;
    }

    @Override
    public VariableInstance getVariableInstance(String variableName) {

        return null;
    }

    @Override
    public Object getVariable(String variableName, boolean fetchAllVariables) {

        return null;
    }

    @Override
    public VariableInstance getVariableInstance(String variableName, boolean fetchAllVariables) {

        return null;
    }

    @Override
    public VariableInstance getVariableInstanceLocal(String variableName) {

        return null;
    }

    @Override
    public Object getVariableLocal(String variableName, boolean fetchAllVariables) {

        return null;
    }

    @Override
    public VariableInstance getVariableInstanceLocal(String variableName, boolean fetchAllVariables) {

        return null;
    }

    @Override
    public <T> T getVariable(String variableName, Class<T> variableClass) {

        return null;
    }

    @Override
    public <T> T getVariableLocal(String variableName, Class<T> variableClass) {

        return null;
    }

    @Override
    public void setVariable(String variableName, Object value, boolean fetchAllVariables) {

    }

    @Override
    public Object setVariableLocal(String variableName, Object value, boolean fetchAllVariables) {

        return null;
    }

    @Override
    public String getSuperExecutionId() {

        return null;
    }

    @Override
    public Object getTransientVariable(String arg0) {

        return null;
    }

    @Override
    public Object getTransientVariableLocal(String arg0) {

        return null;
    }

    @Override
    public Map<String, Object> getTransientVariables() {

        return null;
    }

    @Override
    public Map<String, Object> getTransientVariablesLocal() {

        return null;
    }

    @Override
    public void removeTransientVariable(String arg0) {

    }

    @Override
    public void removeTransientVariableLocal(String arg0) {

    }

    @Override
    public void removeTransientVariables() {

    }

    @Override
    public void removeTransientVariablesLocal() {

    }

    @Override
    public void setTransientVariable(String arg0, Object arg1) {

    }

    @Override
    public void setTransientVariableLocal(String arg0, Object arg1) {

    }

    @Override
    public void setTransientVariables(Map<String, Object> arg0) {

    }

    @Override
    public void setTransientVariablesLocal(Map<String, Object> arg0) {

    }

    @Override
    public String getRootProcessInstanceId() {

        return null;
    }

    @Override
    public void setEventName(String eventName) {

    }

    @Override
    public String getProcessInstanceBusinessKey() {

        return null;
    }

    @Override
    public FlowElement getCurrentFlowElement() {
        FlowElement mockFlowElement = Mockito.mock(FlowElement.class);
        Mockito.when(mockFlowElement.getName())
               .thenReturn("Default Name");
        return mockFlowElement;
    }

    @Override
    public void setCurrentFlowElement(FlowElement flowElement) {

    }

    @Override
    public FlowableListener getCurrentFlowableListener() {

        return null;
    }

    @Override
    public void setCurrentFlowableListener(FlowableListener currentListener) {

    }

    @Override
    public DelegateExecution getParent() {

        return null;
    }

    @Override
    public List<? extends DelegateExecution> getExecutions() {

        return null;
    }

    @Override
    public void setActive(boolean isActive) {

    }

    @Override
    public boolean isActive() {

        return false;
    }

    @Override
    public boolean isEnded() {

        return false;
    }

    @Override
    public void setConcurrent(boolean isConcurrent) {

    }

    @Override
    public boolean isConcurrent() {

        return false;
    }

    @Override
    public boolean isProcessInstanceType() {

        return false;
    }

    @Override
    public void inactivate() {

    }

    @Override
    public boolean isScope() {

        return false;
    }

    @Override
    public void setScope(boolean isScope) {

    }

    @Override
    public boolean isMultiInstanceRoot() {

        return false;
    }

    @Override
    public void setMultiInstanceRoot(boolean isMultiInstanceRoot) {

    }

}
