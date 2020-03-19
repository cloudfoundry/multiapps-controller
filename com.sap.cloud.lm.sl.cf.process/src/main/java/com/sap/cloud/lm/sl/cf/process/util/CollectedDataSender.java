package com.sap.cloud.lm.sl.cf.process.util;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.Marshaller;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.collectors.AnalyticsCollector;
import com.sap.cloud.lm.sl.cf.process.analytics.collectors.GeneralScenarioDetailsCollector;
import com.sap.cloud.lm.sl.cf.process.analytics.model.Analysis;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AnalyticsData;
import com.sap.cloud.lm.sl.cf.process.analytics.model.GeneralProcess;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.common.util.XmlUtil;

@Named("collectedDataSender")
public class CollectedDataSender {

    private static final String XS2 = "XS2";

    @Inject
    private AnalyticsCollector analytics;
    @Inject
    private ApplicationConfiguration configuration;

    @Inject
    private GeneralScenarioDetailsCollector details;

    public AnalyticsData collectAnalyticsData(DelegateExecution execution, Operation.State processState) {
        AnalyticsData model = analytics.collectAnalyticsData(execution);
        model.setProcessFinalState(processState);
        return model;
    }

    public Map<String, Object> getXmlProperties() {
        Map<String, Object> properties = new TreeMap<>();
        properties.put(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "SLAnalytics.xsd");
        properties.put(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        return properties;
    }

    public String convertCollectedAnalyticsDataToXml(DelegateExecution execution, AnalyticsData collectAnalyticsData) {
        Analysis analysis = new Analysis();
        analysis.setTitle("XSA Deploy Service");
        analysis.setGeneralScenarioDetails(details.collectDetails(execution, new GeneralProcess(collectAnalyticsData)));
        return XmlUtil.toXml(analysis, getXmlProperties());
    }

    public void sendCollectedData(RestTemplate restTemplate, String collectedXmlData) {
        Map<String, String> params = new HashMap<>();
        params.put(Constants.TOOL_TYPE, XS2);
        params.put(Constants.FEEDBACK_MAIL, collectedXmlData);

        restTemplate.postForLocation(configuration.getMailApiUrl(), params);
    }
}
