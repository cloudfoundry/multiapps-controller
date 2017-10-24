package com.sap.cloud.lm.sl.cf.process.analytics.model;

import java.time.ZonedDateTime;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class AnalyticsData {

    private String processId;
    private ProcessType processType;
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private long processDurationInSeconds;
    private State processFinalState;
    private String mtaId;
    private String org;
    private String space;
    private String targetURL;

    private Map<String, Object> processSpecificAttributes;

    public AnalyticsData(String processId, ProcessType processType, ZonedDateTime startTime, ZonedDateTime endTime,
        long processDurationInSeconds, State processFinalState, String mtaId, String org, String space, String targetURL,
        Map<String, Object> processSpecificAttributes) {
        this.processId = processId;
        this.processType = processType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.processDurationInSeconds = processDurationInSeconds;
        this.processFinalState = processFinalState;
        this.mtaId = mtaId;
        this.org = org;
        this.space = space;
        this.targetURL = targetURL;
        this.processSpecificAttributes = processSpecificAttributes;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public ProcessType getProcessType() {
        return processType;
    }

    public void setProcessType(ProcessType processType) {
        this.processType = processType;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    public long getProcessDuration() {
        return processDurationInSeconds;
    }

    public void setProcessDuration(long processDuration) {
        this.processDurationInSeconds = processDuration;
    }

    public State getProcessFinalState() {
        return processFinalState;
    }

    public void setProcessFinalState(State processFinalState) {
        this.processFinalState = processFinalState;
    }

    public String getMtaId() {
        return mtaId;
    }

    public void setMtaId(String mtaId) {
        this.mtaId = mtaId;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getTargetUrl() {
        return targetURL;
    }

    public void setTargetUrl(String targetURL) {
        this.targetURL = targetURL;
    }

    public Map<String, Object> getProcessSpecificAttributes() {
        return processSpecificAttributes;
    }

    public void setProcessSpecificAttributes(Map<String, Object> processSpecificAttributes) {
        this.processSpecificAttributes = processSpecificAttributes;
    }
}
