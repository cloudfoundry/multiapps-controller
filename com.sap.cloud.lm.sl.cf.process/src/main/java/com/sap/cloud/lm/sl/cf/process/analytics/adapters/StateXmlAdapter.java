package com.sap.cloud.lm.sl.cf.process.analytics.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

public class StateXmlAdapter extends XmlAdapter<String, Operation.State> {

    @Override
    public String marshal(Operation.State state) {
        return state != null ? state.toString() : null;
    }

    @Override
    public Operation.State unmarshal(String stringState) {
        return stringState == null || stringState.isEmpty() ? null : Operation.State.fromValue(stringState);
    }
}
