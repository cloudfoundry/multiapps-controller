package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "")
@XmlEnum(State.class)
public enum State {
    RUNNING,
    FINISHED,
    ERROR,
    ABORTED,
    ACTION_REQUIRED;

    public static State fromValue(String v) {
        for (State b : State.values()) {
            if (b.name().equals(v)) {
                return b;
            }
        }
        return null;
    }

    public static List<State> getActiveStates() {
        return Arrays.asList(RUNNING, ERROR, ACTION_REQUIRED);
    }

    public static List<State> getFinishedStates() {
        return Arrays.asList(FINISHED, ABORTED);
    }
}
