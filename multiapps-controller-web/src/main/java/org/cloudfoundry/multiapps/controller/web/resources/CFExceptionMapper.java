package org.cloudfoundry.multiapps.controller.web.resources;

import java.sql.SQLException;

import jakarta.servlet.ServletException;
import org.apache.ibatis.exceptions.PersistenceException;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
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

        if (e instanceof CloudOperationException cloudOperationException) {
            status = cloudOperationException.getStatusCode();
        }
        if (e instanceof ServletException servletException) {
            status = handleServletExceptions(servletException);
        }
        if (e instanceof SLException slException) {
            status = handleSLExceptions(slException);
        }
        if (e instanceof ResponseStatusException responseStatusException) {
            status = HttpStatus.valueOf(responseStatusException.getStatusCode()
                                                               .value());
            message = responseStatusException.getReason();
        }
        if (e instanceof SQLException || e instanceof PersistenceException) {
            message = Messages.TEMPORARY_PROBLEM_WITH_PERSISTENCE_LAYER;
        }
        if (e instanceof IllegalArgumentException || e instanceof HttpMessageNotReadableException) {
            status = HttpStatus.BAD_REQUEST;
        }

        LOGGER.error(Messages.ERROR_EXECUTING_REST_API_CALL, e);
        return ResponseEntity.status(status)
                             .body(message);
    }

    private HttpStatus handleServletExceptions(ServletException e) {
        if (e instanceof HttpMediaTypeNotSupportedException) {
            return HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private HttpStatus handleSLExceptions(SLException e) {
        if (e instanceof ContentException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (e instanceof NotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (e instanceof ConflictException) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

}