package com.sap.cloud.lm.sl.cf.process.analytics.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.sap.cloud.lm.sl.cf.process.analytics.adapters.ProcessTypeXmlAdapter;
import com.sap.cloud.lm.sl.cf.process.analytics.adapters.StateXmlAdapter;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

@XmlRootElement(name = "processItem")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class AnalyticsData {

    @XmlElement(name = "processnr")
    private String processId;

    @XmlElement(name = "processtype")
    @XmlJavaTypeAdapter(ProcessTypeXmlAdapter.class)
    private ProcessType processType;

    @XmlElement(name = "startdate")
    private long startTime;

    @XmlElement(name = "enddate")
    private long endTime;

    @XmlElement(name = "processDurationInSeconds")
    private long processDurationInSeconds;

    @XmlElement(name = "processFinalState")
    @XmlJavaTypeAdapter(StateXmlAdapter.class)
    private State processFinalState;

    @XmlElement(name = "mtaId")
    private String mtaId;

    @XmlElement(name = "platform")
    private String platform;

    @XmlElement(name = "org")
    private String org;

    @XmlElement(name = "space")
    private String space;

    @XmlElement(name = "controllerUrl")
    private String controllerUrl;

    @XmlElements({ @XmlElement(name = "processSpecificAttributes", type = DeployProcessAttributes.class),
        @XmlElement(name = "processSpecificAttributes", type = UndeployProcessAttributes.class) })
    private AbstractCommonProcessAttributes processSpecificAttributes;

    public AnalyticsData() {

    }

    public AnalyticsData(String processId, ProcessType processType, long startTime, long endTime, long processDurationInSeconds,
                         State processFinalState, String mtaId, String platform, String org, String space, String controllerUrl,
                         AbstractCommonProcessAttributes commonProcessVariables) {
        this.processId = processId;
        this.processType = processType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.processDurationInSeconds = processDurationInSeconds;
        this.processFinalState = processFinalState;
        this.mtaId = mtaId;
        this.platform = platform;
        this.org = org;
        this.space = space;
        this.controllerUrl = controllerUrl;
        this.processSpecificAttributes = commonProcessVariables;
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

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
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

    public String getControllerUrl() {
        return controllerUrl;
    }

    public void setControllerUrl(String controllerUrl) {
        this.controllerUrl = controllerUrl;
    }

    public AbstractCommonProcessAttributes getProcessSpecificAttributes() {
        return processSpecificAttributes;
    }

    public void setProcessSpecificAttributes(AbstractCommonProcessAttributes processSpecificAttributes) {
        this.processSpecificAttributes = processSpecificAttributes;
    }

}
