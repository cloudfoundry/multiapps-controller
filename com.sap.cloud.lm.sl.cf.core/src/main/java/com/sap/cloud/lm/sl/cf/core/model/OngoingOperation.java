package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Arrays;
import java.util.List;

import com.sap.lmsl.slp.SlpTaskState;

public class OngoingOperation {

    public static class SlpTaskStates {
        public static List<SlpTaskState> getActiveSlpTaskStates() {
            return Arrays.asList(SlpTaskState.SLP_TASK_STATE_RUNNING, SlpTaskState.SLP_TASK_STATE_ERROR, SlpTaskState.SLP_TASK_STATE_DIALOG,
                SlpTaskState.SLP_TASK_STATE_ACTION_REQUIRED, SlpTaskState.SLP_TASK_STATE_INITIAL);
        }

        public static List<SlpTaskState> getFinishedSlpTaskStates() {
            return Arrays.asList(SlpTaskState.SLP_TASK_STATE_FINISHED, SlpTaskState.SLP_TASK_STATE_ABORTED);
        }
    }

    private String processId;
    private ProcessType processType;
    private String startedAt;
    private String spaceId;
    private String mtaId;
    private String user;
    private boolean acquiredLock;
    private SlpTaskState finalState;

    public OngoingOperation(String processId, ProcessType processType, String startedAt, String spaceId, String mtaId, String user,
        boolean acquiredLock, SlpTaskState finalState) {
        this.processId = processId;
        this.processType = processType;
        this.startedAt = startedAt;
        this.spaceId = spaceId;
        this.mtaId = mtaId;
        this.user = user;
        this.acquiredLock = acquiredLock;
        this.finalState = finalState;
    }

    public String getProcessId() {
        return processId;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getMtaId() {
        return mtaId;
    }

    public String getUser() {
        return user;
    }

    public boolean hasAcquiredLock() {
        return acquiredLock;
    }

    public SlpTaskState getFinalState() {
        return this.finalState;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public void setProcessType(ProcessType processType) {
        this.processType = processType;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public void setSpaceId(String spaceId) {
        this.spaceId = spaceId;
    }

    public void setMtaId(String mtaId) {
        this.mtaId = mtaId;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setHasAcquiredLock(boolean acquiredLock) {
        this.acquiredLock = acquiredLock;
    }

    public void setFinalState(SlpTaskState finalState) {
        this.finalState = finalState;
    }

}
