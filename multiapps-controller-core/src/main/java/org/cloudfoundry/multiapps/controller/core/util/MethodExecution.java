package org.cloudfoundry.multiapps.controller.core.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;

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

    public static MethodExecution<String> fromClientResponse(ClientResponse clientResponse) {
        if (clientResponse == null) {
            return null;
        }
        ExecutionState state = clientResponse.statusCode()
                                             .equals(HttpStatus.ACCEPTED) ? ExecutionState.EXECUTING : ExecutionState.FINISHED;
        String response = clientResponse.bodyToMono(String.class)
                                        .block();
        return new MethodExecution<>(response, state);
    }

    public enum ExecutionState {
        FINISHED, EXECUTING
    }
}
