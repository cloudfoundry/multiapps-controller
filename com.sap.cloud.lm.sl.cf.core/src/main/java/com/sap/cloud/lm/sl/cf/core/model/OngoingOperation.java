package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import com.sap.lmsl.slp.SlpTaskState;

@Entity
@Table(name = "ongoing_operation")
@NamedQueries({
    @NamedQuery(name = "find_mta_lock", query = "SELECT oo FROM OngoingOperation oo WHERE oo.mtaId = :mtaId AND oo.spaceId = :spaceId AND oo.acquiredLock = true"),
    @NamedQuery(name = "find_all", query = "SELECT oo FROM OngoingOperation oo"),
    @NamedQuery(name = "find_all_in_space", query = "SELECT oo FROM OngoingOperation oo WHERE oo.spaceId = :spaceId"),
    @NamedQuery(name = "find_all_in_space_desc", query = "SELECT oo FROM OngoingOperation oo WHERE oo.spaceId = :spaceId order by oo.startedAt DESC"),
    @NamedQuery(name = "find_all_active_in_space", query = "SELECT oo FROM OngoingOperation oo WHERE oo.spaceId = :spaceId AND oo.finalState is NULL"),
    @NamedQuery(name = "find_all_finished_in_space", query = "SELECT oo FROM OngoingOperation oo WHERE oo.spaceId = :spaceId AND oo.finalState is NOT NULL") })
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

    @Id
    @Column(name = "process_id")
    private String processId;

    @Column(name = "process_type")
    @Enumerated(EnumType.STRING)
    private ProcessType processType;

    @Column(name = "started_at")
    private String startedAt;

    @Column(name = "space_id")
    private String spaceId;

    @Column(name = "mta_id")
    private String mtaId;

    @Column(name = "userx")
    private String user;

    @Column(name = "acquired_lock")
    private boolean acquiredLock;

    @Column(name = "final_state")
    @Enumerated(EnumType.STRING)
    private SlpTaskState finalState;

    protected OngoingOperation() {
        // Required by JPA
    }

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
