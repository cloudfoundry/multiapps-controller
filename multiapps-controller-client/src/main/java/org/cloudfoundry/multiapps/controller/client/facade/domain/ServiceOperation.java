package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.text.MessageFormat;
import java.util.Objects;

import org.cloudfoundry.client.v3.LastOperation;

public class ServiceOperation {

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
            throw new IllegalArgumentException(MessageFormat.format("Illegal service operation type: {0}", value));
        }

    }

    public enum State {

        SUCCEEDED("succeeded"), FAILED("failed"), IN_PROGRESS("in progress"), INITIAL("initial");

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
            throw new IllegalArgumentException(MessageFormat.format("Illegal service operation state: {0}", value));
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

    public static ServiceOperation fromLastOperation(LastOperation lastOperation) {
        if (lastOperation == null || lastOperation.getType() == null || lastOperation.getState() == null) {
            return null;
        }
        Type type = Type.fromString(lastOperation.getType());
        State state = State.fromString(lastOperation.getState());
        String description = lastOperation.getDescription();
        return new ServiceOperation(type, description, state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ServiceOperation that = (ServiceOperation) o;
        return type == that.type && state == that.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, state);
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, state);
    }

}
