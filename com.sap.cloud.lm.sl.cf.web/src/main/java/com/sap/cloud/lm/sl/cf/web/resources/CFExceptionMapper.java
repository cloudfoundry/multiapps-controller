package com.sap.cloud.lm.sl.cf.web.resources;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        Status status = Status.INTERNAL_SERVER_ERROR;
        String message = Messages.ERROR_EXECUTING_REST_API_CALL;

        if (t instanceof ContentException) {
            status = Status.BAD_REQUEST;
        } else if (t instanceof NotFoundException) {
            status = Status.NOT_FOUND;
        } else if (t instanceof ConflictException) {
            status = Status.CONFLICT;
        }

        if (t instanceof SLException) {
            message = t.getMessage();
        }

        LOGGER.error(Messages.ERROR_EXECUTING_REST_API_CALL, t);
        return Response.status(status)
                       .entity(message)
                       .type(MediaType.TEXT_PLAIN_TYPE)
                       .build();
    }

}