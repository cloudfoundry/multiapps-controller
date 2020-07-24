package org.cloudfoundry.multiapps.controller.web.resources;

import java.sql.SQLException;

import org.apache.ibatis.exceptions.PersistenceException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class CFExceptionMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFExceptionMapper.class);

    @ExceptionHandler
    public ResponseEntity<String> handleException(Exception e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = e.getMessage();

        if (e instanceof CloudOperationException) {
            status = ((CloudOperationException) e).getStatusCode();
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
        if (e instanceof SQLException || e instanceof PersistenceException) {
            message = Messages.TEMPORARY_PROBLEM_WITH_PERSISTENCE_LAYER;
        }

        LOGGER.error(Messages.ERROR_EXECUTING_REST_API_CALL, e);
        return ResponseEntity.status(status)
                             .body(message);
    }

}