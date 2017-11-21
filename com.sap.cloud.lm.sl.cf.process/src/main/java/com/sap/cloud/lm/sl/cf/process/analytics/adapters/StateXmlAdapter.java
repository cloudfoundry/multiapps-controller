package com.sap.cloud.lm.sl.cf.process.analytics.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class StateXmlAdapter extends XmlAdapter<String, State> {

    @Override
    public String marshal(State state) throws Exception {
        return state != null ? state.toString() : null;
    }

    @Override
    public State unmarshal(String stringState) throws Exception {
        return stringState == null || stringState.isEmpty() ? null : State.fromValue(stringState);
    }
}
