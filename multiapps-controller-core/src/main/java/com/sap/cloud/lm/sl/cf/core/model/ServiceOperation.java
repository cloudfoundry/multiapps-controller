package com.sap.cloud.lm.sl.cf.core.model;

import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.sap.cloud.lm.sl.cf.core.Messages;

public class ServiceOperation {

    public static final String LAST_SERVICE_OPERATION = "last_operation";
    public static final String SERVICE_OPERATION_TYPE = "type";
    public static final String SERVICE_OPERATION_STATE = "state";
    public static final String SERVICE_OPERATION_DESCRIPTION = "description";

    public enum Type {

        CREATE, UPDATE, DELETE;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        public static Type fromString(String value) {
            for (Type type : Type.values()) {
                if (type.toString()
                        .equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(MessageFormat.format(Messages.ILLEGAL_SERVICE_OPERATION_TYPE, value));
        }

    }

    public enum State {

        SUCCEEDED("succeeded"), FAILED("failed"), IN_PROGRESS("in progress");

        private final String name;

        State(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static State fromString(String value) {
            for (State state : State.values()) {
                if (state.toString()
                         .equals(value)) {
                    return state;
                }
            }
            throw new IllegalArgumentException(MessageFormat.format(Messages.ILLEGAL_SERVICE_OPERATION_STATE, value));
        }

    }

    private Type type;
    private String description;
    private State state;

    ServiceOperation() {
        // Required by Jackson.
    }

    public ServiceOperation(Type type, String description, State state) {
        this.type = type;
        this.description = description;
        this.state = state;
    }

    public Type getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public State getState() {
        return state;
    }

    public static ServiceOperation fromMap(Map<String, Object> serviceOperation) {
        Type type = Type.fromString(MapUtils.getString(serviceOperation, SERVICE_OPERATION_TYPE));
        State state = State.fromString(MapUtils.getString(serviceOperation, SERVICE_OPERATION_STATE));
        String description = MapUtils.getString(serviceOperation, SERVICE_OPERATION_DESCRIPTION);
        return new ServiceOperation(type, description, state);
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, state);
    }

}
