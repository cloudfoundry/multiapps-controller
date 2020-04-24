package com.sap.cloud.lm.sl.cf.web.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;

public class CFExceptionMapperTest {

    private final CFExceptionMapper exceptionMapper = new CFExceptionMapper();

    @ParameterizedTest
    @MethodSource
    public void testHandleException(Throwable throwable, RestResponse expectedResponse) {
        ResponseEntity<String> response = exceptionMapper.handleException(throwable);
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
            Arguments.of(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"), new RestResponse(500, "Something went wrong"))
        // @formatter:on
        );
    }

}
