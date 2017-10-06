package com.sap.cloud.lm.sl.cf.web.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

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

        private String targetName;
        private String targetXml;

        public PostRequestInput(String targetName, String targetXml) {
            this.targetName = targetName;
            this.targetXml = targetXml;
        }

        public String getTargetName() {
            return targetName;
        }

        public String getPlatformXml() {
            return targetXml;
        }

    }

    protected static class DeleteRequestInput extends RequestInput {

        private String targetName;

        public DeleteRequestInput(String targetName) {
            this.targetName = targetName;
        }

        public String getTargetName() {
            return targetName;
        }

    }

    protected String targetsJson;
    protected RequestInput input;
    protected RestResponse expected;

    protected List<PersistentObject<? extends Target>> targets;

    public TargetPlatformsResourceTest(String targetsJson, RequestInput input, RestResponse expected) {
        this.targetsJson = targetsJson;
        this.input = input;
        this.expected = expected;
    }

    protected abstract DeployTargetDao getDao();

    protected abstract TargetPlatformsResource getResource();

    @Test
    public void testGetAllPlatforms() throws Exception {
        assumeTrue(input instanceof GetAllRequestInput);

        when(getDao().findAll()).thenReturn(targets);

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

        PersistentObject<? extends Target> platform = findByName(targets, getRequestInput.getPlatformName());
        if (platform == null) {
            when(getDao().findByName(getRequestInput.getPlatformName())).thenThrow(
                new NotFoundException(Messages.DEPLOY_TARGET_WITH_NAME_NOT_FOUND, getRequestInput.getPlatformName()));
        } else {
            Mockito.doReturn(platform).when(getDao()).findByName(getRequestInput.getPlatformName());
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

        if (findByName(targets, postRequestInput.getTargetName()) != null) {
            doThrow(new ConflictException(Messages.DEPLOY_TARGET_ALREADY_EXISTS, postRequestInput.getTargetName())).when(getDao()).add(
                any(Target.class));
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

        PersistentObject<? extends Target> target = findByName(targets, putRequestInput.getOldPlatformName());
        if (target != null) {
            Mockito.doReturn(target).when(getDao()).findByName(Mockito.any(String.class));
        }
        if (findByName(targets, putRequestInput.getNewPlatformName()) != null) {
            doThrow(new ConflictException(Messages.DEPLOY_TARGET_ALREADY_EXISTS, putRequestInput.getNewPlatformName())).when(
                getDao()).merge(Mockito.anyLong(), any(Target.class));
        }

        if (findByName(targets, putRequestInput.getOldPlatformName()) == null) {
            doThrow(new NotFoundException(Messages.DEPLOY_TARGET_NOT_FOUND, putRequestInput.getOldPlatformName())).when(getDao()).merge(
                Mockito.anyLong(), any(Target.class));
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

        PersistentObject<? extends Target> target = findByName(targets, deleteRequestInput.getTargetName());

        if (target == null) {
            doThrow(new NotFoundException(Messages.DEPLOY_TARGET_WITH_NAME_NOT_FOUND, deleteRequestInput.getTargetName())).when(
                getDao()).findByName(Mockito.anyString());
        } else {
            Mockito.doReturn(target).when(getDao()).findByName(Mockito.anyString());
        }

        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getResource().deletePlatform(deleteRequestInput.getTargetName());

                assertEquals(expected.getStatus(), response.getStatus());

                return getNonNullEntity(response);
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), getClass(), false);
    }

    protected String getNonNullEntity(Response response) {
        return response.getEntity() != null ? response.getEntity().toString() : "";
    }

    private static PersistentObject<? extends Target> findByName(List<PersistentObject<? extends Target>> targets, String name) {
        for (PersistentObject<? extends Target> t : targets) {
            if (t.getObject().getName().equals(name)) {
                return t;
            }
        }
        return null;
    }

}
