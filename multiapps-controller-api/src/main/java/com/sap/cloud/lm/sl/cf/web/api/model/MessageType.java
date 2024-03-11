package com.sap.cloud.lm.sl.cf.web.api.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "")
@XmlEnum(MessageType.class)
public enum MessageType {

    @XmlEnumValue("INFO")
    INFO(String.valueOf("INFO")), @XmlEnumValue("ERROR")
    ERROR(String.valueOf("ERROR")), @XmlEnumValue("WARNING")
    WARNING(String.valueOf("WARNING")), @XmlEnumValue("EXT")
    EXT(String.valueOf("EXT")), @XmlEnumValue("TASK_STARTUP")
    TASK_STARTUP(String.valueOf("TASK_STARTUP"));

    private String value;

    MessageType(String v) {
        value = v;
    }

    public static MessageType fromValue(String v) {
        for (MessageType b : MessageType.values()) {
            if (String.valueOf(b.value)
                      .equals(v)) {
                return b;
            }
        }
        return null;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
