package com.sap.cloud.lm.sl.cf.process.analytics.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "general_process")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class GeneralProcess {

    @XmlElement(name = "processItem")
    private AnalyticsData analyticsData;

    public GeneralProcess() {
        super();
        // TODO Auto-generated constructor stub
    }

    public GeneralProcess(AnalyticsData analyticsData) {
        super();
        this.analyticsData = analyticsData;
    }

    public AnalyticsData getAnalyticsData() {
        return analyticsData;
    }

    public void setAnalyticsData(AnalyticsData analyticsData) {
        this.analyticsData = analyticsData;
    }

}
