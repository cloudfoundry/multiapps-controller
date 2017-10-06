package com.sap.cloud.lm.sl.cf.web.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.DeployTargetDto;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.XmlUtil;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public abstract class DeployTargetsResourceTest {

    protected static class RequestInput {

    }

    protected static class GetRequestInput extends RequestInput {

        private long targetId;

        public GetRequestInput(long targetId) {
            this.targetId = targetId;
        }

        public long getTargetId() {
            return targetId;
        }

    }

    protected static class GetAllRequestInput extends RequestInput {

        public GetAllRequestInput() {

        }

    }

    protected static class PutRequestInput extends RequestInput {

        private long targetId;
        private String targetXml;
        private String updatedTargetXml;

        public PutRequestInput(long targetId, String targetXml, String updatedTargetXml) {
            this.targetId = targetId;
            this.targetXml = targetXml;
            this.updatedTargetXml = updatedTargetXml;
        }

        public long getTargetId() {
            return targetId;
        }

        public String getTargetXml() {
            return targetXml;
        }

        public String getUpdatedTargetXml() {
            return updatedTargetXml;
        }

    }

    protected static class PostRequestInput extends RequestInput {

        private String targetName;
        private String targetXml;
        private String createdTargetXml;

        public PostRequestInput(String targetName, String targetXml, String createdTargetXml) {
            this.targetName = targetName;
            this.targetXml = targetXml;
            this.createdTargetXml = createdTargetXml;
        }

        public String getTargetName() {
            return targetName;
        }

        public String getTargetXml() {
            return targetXml;
        }

        public String getCreatedTargetXml() {
            return createdTargetXml;
        }

    }

    protected static class DeleteRequestInput extends RequestInput {

        private long targetId;

        public DeleteRequestInput(long targetId) {
            this.targetId = targetId;
        }

        public long getTargetId() {
            return targetId;
        }

    }

    protected String targetsJson;
    protected RequestInput input;
    protected RestResponse expected;

    protected List<PersistentObject<? extends Target>> targets;

    public DeployTargetsResourceTest(String targetsJson, RequestInput input, RestResponse expected) {
        this.targetsJson = targetsJson;
        this.input = input;
        this.expected = expected;
    }

    protected abstract DeployTargetDao getDao();

    protected abstract DeployTargetsResource getResource();

    protected abstract DeployTargetDto deployTargetDtoFromXml(String path);

    @Test
    public void testGetAllTargets() throws Exception {
        assumeTrue(input instanceof GetAllRequestInput);

        when(getDao().findAll()).thenReturn(targets);

        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getResource().getTargets(null);

                assertEquals(expected.getStatus(), response.getStatus());

                return getNonNullEntity(response);
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), getClass(), false);
    }

    @Test
    public void testGetTarget() throws Exception {
        assumeTrue(input instanceof GetRequestInput);

        GetRequestInput getRequestInput = (GetRequestInput) input;

        PersistentObject<? extends Target> target = findById(targets, getRequestInput.getTargetId());
        if (target == null) {
            when(getDao().find(getRequestInput.getTargetId())).thenThrow(
                new NotFoundException(Messages.DEPLOY_TARGET_NOT_FOUND, getRequestInput.getTargetId()));
        } else {
            Mockito.doReturn(target).when(getDao()).find(getRequestInput.getTargetId());
        }

        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getResource().getTarget(getRequestInput.getTargetId());

                assertEquals(expected.getStatus(), response.getStatus());

                return getNonNullEntity(response);
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), getClass(), false);
    }

    @Test
    public void testCreateTarget() throws Exception {
        assumeTrue(input instanceof PostRequestInput);

        PostRequestInput postRequestInput = (PostRequestInput) input;

        String createdTargetXml = postRequestInput.getCreatedTargetXml();
        if (createdTargetXml != "") {
            DeployTargetDto createdTargetDto = deployTargetDtoFromXml(createdTargetXml);
            Mockito.doReturn(createdTargetDto.toDeployTarget()).when(getDao()).add(Mockito.any(Target.class));
        } else {
            Mockito.doThrow(new ConflictException(Messages.DEPLOY_TARGET_ALREADY_EXISTS, postRequestInput.getTargetName())).when(
                getDao()).add(Mockito.any(Target.class));
        }

        Class<? extends DeployTargetsResourceTest> clazz = getClass();
        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                String inputTargetXml = TestUtil.getResourceAsString(postRequestInput.getTargetXml(), clazz);
                Response response = getResource().createTarget(inputTargetXml);

                assertEquals(expected.getStatus(), response.getStatus());

                String entity = getNonNullEntity(response);
                return entity;
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), clazz, false);
    }

    @Test
    public void testUpdateTarget() throws Exception {
        assumeTrue(input instanceof PutRequestInput);

        PutRequestInput putRequestInput = (PutRequestInput) input;

        if (putRequestInput.getUpdatedTargetXml() != "") {
            String updatedTargetXml = putRequestInput.getUpdatedTargetXml();
            DeployTargetDto updatedTargetDto = deployTargetDtoFromXml(updatedTargetXml);
            // XmlUtil.fromXml(getClass().getResourceAsStream(updatedTargetXml), DeployTargetDto.class);
            PersistentObject<? extends Target> updatedTarget = updatedTargetDto.toDeployTarget();

            Mockito.doReturn(updatedTarget).when(getDao()).merge(Mockito.anyLong(), Mockito.any(Target.class));
        } else {
            Target target = XmlUtil.fromXml(getClass().getResourceAsStream(putRequestInput.getTargetXml()), Target.class);
            if (findByName(targets, target.getName()) != null) {
                doThrow(new ConflictException(Messages.DEPLOY_TARGET_ALREADY_EXISTS, target.getName())).when(getDao()).merge(
                    Mockito.anyLong(), Mockito.any(Target.class));
            }
        }

        Class<? extends DeployTargetsResourceTest> clazz = getClass();
        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getResource().updateTarget(putRequestInput.getTargetId(),
                    TestUtil.getResourceAsString(putRequestInput.getTargetXml(), clazz));

                assertEquals(expected.getStatus(), response.getStatus());

                return getNonNullEntity(response);
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), clazz, false);
    }

    @Test
    public void testDeleteTarget() throws Exception {
        assumeTrue(input instanceof DeleteRequestInput);

        DeleteRequestInput deleteRequestInput = (DeleteRequestInput) input;

        if (findById(targets, deleteRequestInput.getTargetId()) == null) {
            doThrow(new NotFoundException(Messages.DEPLOY_TARGET_NOT_FOUND, deleteRequestInput.getTargetId())).when(getDao()).remove(
                deleteRequestInput.getTargetId());
        }

        TestUtil.test(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Response response = getResource().deleteTarget(deleteRequestInput.getTargetId());

                assertEquals(expected.getStatus(), response.getStatus());

                return getNonNullEntity(response);
            }
        }, expected.getEntity() != null ? expected.getEntity().toString() : expected.getErrorMessage(), getClass(), false);
    }

    protected String getNonNullEntity(Response response) {
        return response.getEntity() != null ? response.getEntity().toString() : "";
    }

    private static PersistentObject<? extends Target> findById(List<PersistentObject<? extends Target>> targets, long id) {
        for (PersistentObject<? extends Target> t : targets) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
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
