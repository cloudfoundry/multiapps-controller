package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.text.MessageFormat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.cloudfoundry.multiapps.controller.Messages;

public enum ServiceInstanceType {

    MANAGED("managed"), USER_PROVIDED("user-provided");

    private final String value;

    ServiceInstanceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ServiceInstanceType from(String value) {
        for (ServiceInstanceType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException(MessageFormat.format(Messages.UNKNOWN_SERVICE_INSTANCE_TYPE_0, value));
    }
}
