package com.sap.cloud.lm.sl.cf.core.exec;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.sap.cloud.lm.sl.cf.core.exec.MethodExecution.ExecutionState;

public class MethodExecution<T> {

    private ExecutionState state;
    private T response;
    
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
        if(response == null) {
            return null;
        }
        ExecutionState state = response.getStatusCode().equals(HttpStatus.ACCEPTED) ? ExecutionState.EXECUTING : ExecutionState.FINISHED;
        String responseEntity = response.getBody();
        return new MethodExecution<String>(responseEntity, state);
    }

    public enum ExecutionState {
        FINISHED, EXECUTING;
    }
}
