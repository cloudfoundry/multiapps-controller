package com.sap.cloud.lm.sl.cf.web.resources;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.ResponseRenderer;
import com.sap.cloud.lm.sl.common.util.Runnable;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CFExceptionMapperTest {

    private CFExceptionMapper exceptionMapper;
    private Throwable throwable;
    private RestResponse expectedResponse;

    public CFExceptionMapperTest(Throwable throwable, RestResponse expectedResponse) {
        this.throwable = throwable;
        this.expectedResponse = expectedResponse;
        this.exceptionMapper = new CFExceptionMapper();
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            { new NotFoundException("Not found"), new RestResponse(404, "Not found"), },
            { new ConflictException("Already exists"), new RestResponse(409, "Already exists"), },
            { new ParsingException("Bad request"), new RestResponse(400, "Bad request"), },
            { new ContentException("Bad request"), new RestResponse(400, "Bad request"), },
            { new SLException("SL exception"), new RestResponse(500, "SL exception"), },
            { new RuntimeException("not printed"), new RestResponse(500, Messages.ERROR_EXECUTING_REST_API_CALL), },
            { new WebApplicationException(ResponseRenderer.renderResponseForStatus(406, "Not modified message")), new RestResponse(406, "Not modified message"), },
         // @formatter:on
        });
    }

    @Test
    public void test() throws Exception {
        TestUtil.test(new Runnable() {

            @Override
            public void run() throws Exception {
                Response response = exceptionMapper.toResponse(throwable);
                assertEquals(expectedResponse.getStatus(), response.getStatus());
                assertEquals(expectedResponse.getEntity(), response.getEntity());
            }
        }, "");
    }
}
