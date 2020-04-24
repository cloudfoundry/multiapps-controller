package com.sap.cloud.lm.sl.cf.web.resources;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.cf.web.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;

@ControllerAdvice
public class CFExceptionMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFExceptionMapper.class);

    @ExceptionHandler
    public ResponseEntity<String> handleException(Exception e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = e.getMessage();

        if (e instanceof CloudOperationException) {
            status = HttpStatus.BAD_GATEWAY;
        }
        if (e instanceof ContentException || e instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
        }
        if (e instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
        }
        if (e instanceof ConflictException) {
            status = HttpStatus.CONFLICT;
        }
        if (e instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) e;
            status = rse.getStatus();
            message = rse.getReason();
        }

        LOGGER.error(Messages.ERROR_EXECUTING_REST_API_CALL, e);
        return ResponseEntity.status(status)
                             .body(message);
    }

}