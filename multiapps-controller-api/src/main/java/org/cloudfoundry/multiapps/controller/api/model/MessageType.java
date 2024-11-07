package org.cloudfoundry.multiapps.controller.api.model;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "")
@XmlEnum(MessageType.class)
public enum MessageType {

    INFO, ERROR, WARNING, EXT, TASK_STARTUP;

    public static MessageType fromValue(String v) {
        for (MessageType b : MessageType.values()) {
            if (b.name()
                 .equals(v)) {
                return b;
            }
        }
        return null;
    }
}
