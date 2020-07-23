package com.sap.cloud.lm.sl.cf.web.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.stream.Stream;

import org.apache.ibatis.exceptions.PersistenceException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.cf.web.Messages;

public class CFExceptionMapperTest {

    private final CFExceptionMapper exceptionMapper = new CFExceptionMapper();

    @ParameterizedTest
    @MethodSource
    public void testHandleException(Exception exception, RestResponse expectedResponse) {
        ResponseEntity<String> response = exceptionMapper.handleException(exception);
        assertEquals(expectedResponse.getStatus(), response.getStatusCodeValue());
        assertEquals(expectedResponse.getEntity(), response.getBody());
    }

    public static Stream<Arguments> testHandleException() {
        return Stream.of(
        // @formatter:off
            Arguments.of(new NotFoundException("Not found"), new RestResponse(404, "Not found")),
            Arguments.of(new ConflictException("Already exists"), new RestResponse(409, "Already exists")),
            Arguments.of(new ParsingException("Bad request"), new RestResponse(400, "Bad request")),
            Arguments.of(new ContentException("Bad request"), new RestResponse(400, "Bad request")),
            Arguments.of(new SLException("SL exception"), new RestResponse(500, "SL exception")),
            Arguments.of(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"), new RestResponse(404, "Not found")),
            Arguments.of(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request"), new RestResponse(400, "Bad request")),
            Arguments.of(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"), new RestResponse(500, "Something went wrong")),
            Arguments.of(new CloudOperationException(HttpStatus.TOO_MANY_REQUESTS, HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(), "Rate limit exceeded"), new RestResponse(429, "429 Too Many Requests: Rate limit exceeded")),
            Arguments.of(new SQLException(), new RestResponse(500, Messages.TEMPORARY_PROBLEM_WITH_PERSISTENCE_LAYER)),
            Arguments.of(new PersistenceException(), new RestResponse(500, Messages.TEMPORARY_PROBLEM_WITH_PERSISTENCE_LAYER))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testHandleExceptionForCloudOperationExceptionWithAllHttpStatuses(HttpStatus httpStatus) {
        ResponseEntity<String> response = exceptionMapper.handleException(new CloudOperationException(httpStatus,
                                                                                                      httpStatus.getReasonPhrase()));
        StringBuilder expectedMessage = new StringBuilder();
        expectedMessage.append(httpStatus.value());
        expectedMessage.append(" ");
        expectedMessage.append(httpStatus.getReasonPhrase());
        RestResponse expectedResponse = new RestResponse(httpStatus.value(), expectedMessage.toString());

        assertEquals(expectedResponse.getStatus(), response.getStatusCodeValue());
        assertEquals(expectedResponse.getEntity(), response.getBody());
    }

    public static Stream<HttpStatus> testHandleExceptionForCloudOperationExceptionWithAllHttpStatuses() {
        return Stream.of(HttpStatus.values());
    }

}
