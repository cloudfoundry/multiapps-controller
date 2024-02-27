package com.sap.cloud.lm.sl.cf.process.analytics.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "general_scenario_details")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class GeneralScenarioDetails {

    @XmlElement(name = "lmprocess")
    private String lmProcess;
    @XmlElement(name = "startdate")
    private long startDate;
    @XmlElement(name = "enddate")
    private long endDate;
    @XmlElement(name = "modelversion")
    private String modelVersion;
    @XmlElement(name = "general_process")
    private GeneralProcess generalProcess;

    public GeneralScenarioDetails() {
        super();
    }

    public GeneralScenarioDetails(GeneralProcess generalProcess) {
        super();
        this.generalProcess = generalProcess;
    }

    public GeneralScenarioDetails(String lmProcess, long startDate, long endDate, String modelVersion, GeneralProcess generalProcess) {
        this.lmProcess = lmProcess;
        this.startDate = startDate;
        this.endDate = endDate;
        this.modelVersion = modelVersion;
        this.generalProcess = generalProcess;
    }

    public String getLmProcess() {
        return lmProcess;
    }

    public void setLmProcess(String lmProcess) {
        this.lmProcess = lmProcess;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public GeneralProcess getGeneralProcess() {
        return generalProcess;
    }

    public void setGeneralProcess(GeneralProcess generalProcess) {
        this.generalProcess = generalProcess;
    }

}
