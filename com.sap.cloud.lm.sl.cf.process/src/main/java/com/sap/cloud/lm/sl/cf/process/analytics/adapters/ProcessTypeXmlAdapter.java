package com.sap.cloud.lm.sl.cf.process.analytics.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

public class ProcessTypeXmlAdapter extends XmlAdapter<String, ProcessType> {

    @Override
    public String marshal(ProcessType type) {
        return type != null ? type.toString() : null;
    }

    @Override
    public ProcessType unmarshal(String type) {
        return type == null || type.isEmpty() ? null : ProcessType.fromString(type);
    }

}
