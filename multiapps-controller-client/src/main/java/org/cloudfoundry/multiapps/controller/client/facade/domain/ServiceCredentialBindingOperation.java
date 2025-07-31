package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.cloudfoundry.client.v3.LastOperation;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.multiapps.controller.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableServiceCredentialBindingOperation.class)
@JsonDeserialize(as = ImmutableServiceCredentialBindingOperation.class)
public abstract class ServiceCredentialBindingOperation {

    public abstract Type getType();

    public abstract State getState();

    @Nullable
    public abstract String getDescription();

    @Nullable
    public abstract LocalDateTime getCreatedAt();

    @Nullable
    public abstract LocalDateTime getUpdatedAt();

    public static ServiceCredentialBindingOperation from(LastOperation lastOperation) {
        String lastOperationType = lastOperation.getType();
        String lastOperationState = lastOperation.getState();
        String lastOperationDescription = lastOperation.getDescription();
        String lastOperationCreatedAt = lastOperation.getCreatedAt();
        String lastOperationUpdatedAt = lastOperation.getUpdatedAt();
        return ImmutableServiceCredentialBindingOperation.builder()
                                                         .type(ServiceCredentialBindingOperation.Type.fromString(lastOperationType))
                                                         .state(ServiceCredentialBindingOperation.State.fromString(lastOperationState))
                                                         .description(lastOperationDescription)
                                                         .createdAt(LocalDateTime.parse(lastOperationCreatedAt,
                                                                                        DateTimeFormatter.ISO_DATE_TIME))
                                                         .updatedAt(LocalDateTime.parse(lastOperationUpdatedAt,
                                                                                        DateTimeFormatter.ISO_DATE_TIME))
                                                         .build();
    }

    public enum Type {
        CREATE, DELETE;

        public static Type fromString(String value) {
            return Arrays.stream(values())
                         .filter(type -> type.toString()
                                             .equals(value))
                         .findFirst()
                         .orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("Illegal service binding operation type: \"{0}\"",
                                                                                              value)));
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum State {
        INITIAL("initial"), IN_PROGRESS("in progress"), SUCCEEDED("succeeded"), FAILED("failed");

        private final String name;

        State(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static State fromString(String value) {
            return Arrays.stream(values())
                         .filter(state -> state.toString()
                                               .equals(value))
                         .findFirst()
                         .orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("Illegal service binding state: \"{0}\"",
                                                                                              value)));
        }
    }

}
