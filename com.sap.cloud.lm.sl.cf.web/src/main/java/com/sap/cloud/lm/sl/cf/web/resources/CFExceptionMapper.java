package com.sap.cloud.lm.sl.cf.web.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;

@ControllerAdvice
public class CFExceptionMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFExceptionMapper.class);

    @ExceptionHandler({ SLException.class, IllegalArgumentException.class })
    public ResponseEntity<String> handleException(Throwable t) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (t instanceof ContentException || t instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
        }
        if (t instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
        }
        if (t instanceof ConflictException) {
            status = HttpStatus.CONFLICT;
        }
        String message = t.getMessage();

        LOGGER.error(Messages.ERROR_EXECUTING_REST_API_CALL, t);
        return ResponseEntity.status(status)
                             .body(message);
    }

}