package com.sap.cloud.lm.sl.cf.web.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.dao.TargetPlatformDao;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;

public abstract class TargetPlatformsResourceTest {

    protected static class RequestInput {

    }

    protected static class GetRequestInput extends RequestInput {

        private String platformName;

        public GetRequestInput(String platformName) {
            this.platformName = platformName;
        }

        public String getPlatformName() {
            return platformName;
        }

    }

    protected static class GetAllRequestInput extends RequestInput {

        public GetAllRequestInput() {

        }

    }

    protected static class PutRequestInput extends RequestInput {

        private String oldPlatformName;
        private String newPlatformName;
        private String platformXml;

        public PutRequestInput(String oldPlatformName, String newPlatformName, String platformXml) {
            this.oldPlatformName = oldPlatformName;
            this.newPlatformName = newPlatformName;
            this.platformXml = platformXml;
        }

        public String getOldPlatformName() {
            return oldPlatformName;
        }

        public String getNewPlatformName() {
            return newPlatformName;
        }

        public String getPlatformXml() {
            return platformXml;
        }

    }

    protected static class PostRequestInput extends RequestInput {

        private String platformName;
        private String platformXml;

        public PostRequestInput(String platformName, String platformXml) {
            this.platformName = platformName;
            this.platformXml = platformXml;
        }

        public String getPlatformName() {
            return platformName;
        }

        public String getPlatformXml() {
            return platformXml;
        }

    }

    protected static class DeleteRequestInput extends RequestInput {

        private String platformName;

        public DeleteRequestInput(String platformName) {
            this.platformName = platformName;
        }

        public String getPlatformName() {
            return platformName;
        }

    }

    private static final DescriptorHandler HANDLER = new DescriptorHandler();

    protected String platformsJson;
    protected RequestInput input;
    protected RestResponse expected;

    protected List<TargetPlatform> platforms;

    public TargetPlatformsResourceTest(String platformsJson, RequestInput input, RestResponse expected) {
        this.platformsJson = platformsJson;
        this.input = input;
        this.expected = expected;
    }

    protected abstract TargetPlatformDao getDao();

    protected abstract TargetPlatformsResource getResource();

    @Test
    public void testGetAllPlatforms() throws Exception {
        assumeTrue(input instanceof GetAllRequestInput);

        when(getDao().findAll()).thenReturn(platforms);

        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getResource().getAllPlatforms();

                assertEquals(expected.getStatus(), response.getStatus());

                return getNonNullEntity(response);
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), getClass(), false);
    }

    @Test
    public void testGetPlatform() throws Exception {
        assumeTrue(input instanceof GetRequestInput);

        GetRequestInput getRequestInput = (GetRequestInput) input;

        TargetPlatform platform = HANDLER.findPlatform(platforms, getRequestInput.getPlatformName());
        if (platform == null) {
            when(getDao().find(getRequestInput.getPlatformName())).thenThrow(
                new NotFoundException(Messages.TARGET_PLATFORM_NOT_FOUND, getRequestInput.getPlatformName()));
        } else {
            when(getDao().find(getRequestInput.getPlatformName())).thenReturn(platform);
        }

        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getResource().getPlatform(getRequestInput.getPlatformName());

                assertEquals(expected.getStatus(), response.getStatus());

                return getNonNullEntity(response);
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), getClass(), false);
    }

    @Test
    public void testCreatePlatform() throws Exception {
        assumeTrue(input instanceof PostRequestInput);

        PostRequestInput postRequestInput = (PostRequestInput) input;

        if (HANDLER.findPlatform(platforms, postRequestInput.getPlatformName()) != null) {
            doThrow(new ConflictException(Messages.TARGET_PLATFORM_ALREADY_EXISTS, postRequestInput.getPlatformName())).when(getDao()).add(
                any(TargetPlatform.class));
        }

        Class<? extends TargetPlatformsResourceTest> clazz = getClass();
        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getResource().createPlatform(TestUtil.getResourceAsString(postRequestInput.getPlatformXml(), clazz));

                assertEquals(expected.getStatus(), response.getStatus());

                return getNonNullEntity(response);
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), clazz, false);
    }

    @Test
    public void testUpdatePlatform() throws Exception {
        assumeTrue(input instanceof PutRequestInput);

        PutRequestInput putRequestInput = (PutRequestInput) input;

        if (HANDLER.findPlatform(platforms, putRequestInput.getNewPlatformName()) != null) {
            doThrow(new ConflictException(Messages.TARGET_PLATFORM_ALREADY_EXISTS, putRequestInput.getNewPlatformName())).when(
                getDao()).merge(anyString(), any(TargetPlatform.class));
        }

        if (HANDLER.findPlatform(platforms, putRequestInput.getOldPlatformName()) == null) {
            doThrow(new NotFoundException(Messages.TARGET_PLATFORM_NOT_FOUND, putRequestInput.getOldPlatformName())).when(getDao()).merge(
                anyString(), any(TargetPlatform.class));
        }

        Class<? extends TargetPlatformsResourceTest> clazz = getClass();
        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getResource().updatePlatform(putRequestInput.getOldPlatformName(),
                    TestUtil.getResourceAsString(putRequestInput.getPlatformXml(), clazz));

                assertEquals(expected.getStatus(), response.getStatus());

                return getNonNullEntity(response);
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), clazz, false);
    }

    @Test
    public void testDeletePlatform() throws Exception {
        assumeTrue(input instanceof DeleteRequestInput);

        DeleteRequestInput deleteRequestInput = (DeleteRequestInput) input;

        if (HANDLER.findPlatform(platforms, deleteRequestInput.getPlatformName()) == null) {
            doThrow(new NotFoundException(Messages.TARGET_PLATFORM_NOT_FOUND, deleteRequestInput.getPlatformName())).when(getDao()).remove(
                deleteRequestInput.getPlatformName());
        }

        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getResource().deletePlatform(deleteRequestInput.getPlatformName());

                assertEquals(expected.getStatus(), response.getStatus());

                return getNonNullEntity(response);
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), getClass(), false);
    }

    protected String getNonNullEntity(Response response) {
        return response.getEntity() != null ? response.getEntity().toString() : "";
    }

}
