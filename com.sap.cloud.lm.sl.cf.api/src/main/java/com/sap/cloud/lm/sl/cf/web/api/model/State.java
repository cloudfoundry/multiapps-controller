package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "")
@XmlEnum(State.class)
public enum State {

    @XmlEnumValue("RUNNING")
    RUNNING(String.valueOf("RUNNING")), @XmlEnumValue("FINISHED")
    FINISHED(String.valueOf("FINISHED")), @XmlEnumValue("ERROR")
    ERROR(String.valueOf("ERROR")), @XmlEnumValue("ABORTED")
    ABORTED(String.valueOf("ABORTED")), @XmlEnumValue("ACTION_REQUIRED")
    ACTION_REQUIRED(String.valueOf("ACTION_REQUIRED"));

    private String value;

    State(String v) {
        value = v;
    }

    public static State fromValue(String v) {
        for (State b : State.values()) {
            if (String.valueOf(b.value)
                      .equals(v)) {
                return b;
            }
        }
        return null;
    }

    public static List<State> getActiveStates() {
        List<State> activeStates = new ArrayList<>();
        activeStates.add(RUNNING);
        activeStates.add(ERROR);
        activeStates.add(ACTION_REQUIRED);
        return activeStates;
    }

    public static List<State> getFinishedStates() {
        List<State> finishedStates = new ArrayList<>();
        finishedStates.add(FINISHED);
        finishedStates.add(ABORTED);
        return finishedStates;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
