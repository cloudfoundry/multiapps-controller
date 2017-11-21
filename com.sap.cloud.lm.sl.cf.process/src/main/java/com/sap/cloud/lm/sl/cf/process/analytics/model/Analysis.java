package com.sap.cloud.lm.sl.cf.process.analytics.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "analysis")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class Analysis {

    public Analysis() {
        super();
    }

    @XmlElement(name = "title")
    private String title;

    @XmlElement(name = "general_scenario_details")
    private GeneralScenarioDetails generalScenarioDetails;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public GeneralScenarioDetails getGeneralScenarioDetails() {
        return generalScenarioDetails;
    }

    public void setGeneralScenarioDetails(GeneralScenarioDetails generalScenarioDetails) {
        this.generalScenarioDetails = generalScenarioDetails;
    }

}
