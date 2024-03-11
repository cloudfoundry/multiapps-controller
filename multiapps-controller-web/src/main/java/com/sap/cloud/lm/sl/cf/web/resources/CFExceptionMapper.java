package com.sap.cloud.lm.sl.cf.web.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.security.VirusScannerException;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;

public class CFExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFExceptionMapper.class);

    @Override
    public Response toResponse(Throwable t) {
        if (t instanceof WebApplicationException) {
            return ((WebApplicationException) t).getResponse();
        }
        String message = Messages.ERROR_EXECUTING_REST_API_CALL;
        int statusCode = getStatusCode(t);
        if (t instanceof SLException || t instanceof VirusScannerException || t instanceof CloudOperationException) {
            message = t.getMessage();
        }
        LOGGER.error(Messages.ERROR_EXECUTING_REST_API_CALL, t);
        return Response.status(statusCode)
                       .entity(message)
                       .type(MediaType.TEXT_PLAIN_TYPE)
                       .build();
    }

    private int getStatusCode(Throwable t) {
        int statusCode = Status.INTERNAL_SERVER_ERROR.getStatusCode();
        if (t instanceof ContentException) {
            statusCode = Status.BAD_REQUEST.getStatusCode();
        } else if (t instanceof NotFoundException) {
            statusCode = Status.NOT_FOUND.getStatusCode();
        } else if (t instanceof ConflictException) {
            statusCode = Status.CONFLICT.getStatusCode();
        } else if (t instanceof CloudOperationException) {
            statusCode = ((CloudOperationException) t).getStatusCode()
                                                      .value();
        }
        return statusCode;
    }

}