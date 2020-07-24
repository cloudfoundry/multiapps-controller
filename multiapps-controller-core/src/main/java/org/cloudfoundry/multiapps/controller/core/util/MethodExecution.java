package org.cloudfoundry.multiapps.controller.core.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class MethodExecution<T> {

    private final ExecutionState state;
    private final T response;

    public MethodExecution(T response, ExecutionState state) {
        this.response = response;
        this.state = state;
    }

    public ExecutionState getState() {
        return state;
    }

    public T getResponse() {
        return response;
    }

    public static MethodExecution<String> fromResponseEntity(ResponseEntity<String> response) {
        if (response == null) {
            return null;
        }
        ExecutionState state = response.getStatusCode()
                                       .equals(HttpStatus.ACCEPTED) ? ExecutionState.EXECUTING : ExecutionState.FINISHED;
        String responseEntity = response.getBody();
        return new MethodExecution<>(responseEntity, state);
    }

    public enum ExecutionState {
        FINISHED, EXECUTING
    }
}
