package com.sap.cloud.lm.sl.cf.web.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.ResponseRenderer;

public class CFExceptionMapperTest {

    private final CFExceptionMapper exceptionMapper = new CFExceptionMapper();

    @ParameterizedTest
    @MethodSource
    public void testToResponse(Throwable throwable, RestResponse expectedResponse) {
        Response response = exceptionMapper.toResponse(throwable);
        assertEquals(expectedResponse.getStatus(), response.getStatus());
        assertEquals(expectedResponse.getEntity(), response.getEntity());
    }

    public static Stream<Arguments> testToResponse() {
        return Stream.of(
        // @formatter:off
            Arguments.of(new NotFoundException("Not found"), new RestResponse(404, "Not found")),
            Arguments.of(new ConflictException("Already exists"), new RestResponse(409, "Already exists")),
            Arguments.of(new ParsingException("Bad request"), new RestResponse(400, "Bad request")),
            Arguments.of(new ContentException("Bad request"), new RestResponse(400, "Bad request")),
            Arguments.of(new SLException("SL exception"), new RestResponse(500, "SL exception")),
            Arguments.of(new RuntimeException("Not printed"), new RestResponse(500, Messages.ERROR_EXECUTING_REST_API_CALL)),
            Arguments.of(new WebApplicationException(ResponseRenderer.renderResponseForStatus(Status.NOT_ACCEPTABLE, "Not acceptable")), new RestResponse(406, "Not acceptable"))
        // @formatter:on
        );
    }

}
