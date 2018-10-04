package com.sap.cloud.lm.sl.cf.core.dao.v1;

import static com.sap.cloud.lm.sl.common.util.TestUtil.getResourceAsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.v1.DeployTargetDto;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.v1.Target;

@RunWith(Parameterized.class)
public class DeployTargetDaoTest {

    protected static class Input {
    }

    protected static class AddInput extends Input {
        String additionalResource;

        public AddInput(String addtionalResource) {
            this.additionalResource = addtionalResource;
        }

        public String getAdditionalResource() {
            return additionalResource;
        }
    }

    protected static class RemoveInput extends Input {

        private long idToRemove;

        public RemoveInput(long idToRemvoe) {
            this.idToRemove = idToRemvoe;
        }

        private long getIdToRemove() {
            return this.idToRemove;
        }

    }

    protected static class MergeInput extends Input {
        String deployTargetResource;

        public MergeInput(String deployTargetResource) {
            this.deployTargetResource = deployTargetResource;
        }

        public String getDeployTargetResource() {
            return deployTargetResource;
        }

    }

    protected String resource = "/platform/platform-v1.json";
    private String exceptionMessage;

    private Input input;
    protected static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("DeployTargetManagement");
    private com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao<Target, DeployTargetDto> dao = getDeployTargetDao();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public DeployTargetDaoTest(Input input, String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
        this.input = input;
    }

    @Before
    public void setUp() throws Exception {
        Target tp = loadDeployTarget(resource);
        dao.add(tp);
    }

    @Test
    public void testAdd() throws Exception {
        assumeTrue(input instanceof AddInput);
        AddInput addInput = (AddInput) input;
        loadExceptionForInput(addInput);

        Target target1 = loadDeployTarget(addInput.getAdditionalResource());
        PersistentObject<Target> added = dao.add(target1);
        assertTrue(added.getId() != 0);

        List<PersistentObject<Target>> targets = dao.findAll();
        assertEquals(2, targets.size());

        Target target2 = targets.get(1)
            .getObject();

        assertDeployTargets(target1, target2);
        dao.add(target1);
    }

    @Test
    public void testMerge() throws Exception {
        assumeTrue(input instanceof MergeInput);
        MergeInput mergeInput = (MergeInput) input;

        Target target = loadDeployTarget(mergeInput.getDeployTargetResource());

        PersistentObject<? extends Target> merged = dao.merge(dao.findAll()
            .get(0)
            .getId(), target);

        assertDeployTargets(target, dao.find(merged.getId())
            .getObject());
    }

    @Test
    public void testRemove() throws Exception {
        assumeTrue(input instanceof RemoveInput);
        RemoveInput removeInput = (RemoveInput) input;
        loadExceptionForInput(input);

        List<PersistentObject<Target>> targets = dao.findAll();
        assertEquals(1, targets.size());
        dao.remove(targets.get(0)
            .getId());
        assertEquals(0, dao.findAll()
            .size());

        dao.remove(removeInput.getIdToRemove());
    }

    @After
    public void clearDatabase() throws Exception {
        for (PersistentObject<? extends Target> tp : dao.findAll()) {
            dao.remove(tp.getId());
        }
    }

    protected void assertDeployTargets(Target target1, Target target2) {
        String tp1Json = JsonUtil.toJson(target1, true);
        String tp2Json = JsonUtil.toJson(target2, true);

        assertEquals(tp1Json, tp2Json);
    }

    protected Target loadDeployTarget(String deployTargetJsonPath) throws Exception {
        String json = getResourceAsString(deployTargetJsonPath, getClass());
        return getConfigurationParser().parseTargetJson(json);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Add new deploy target:
            {
                 new AddInput("/platform/platform-v1-1.json"), "Deploy target with name \"DEPLOY-TARGET-TEST-1\" already exists"
            },
            // (1) Remove deploy target:
            {
                 new RemoveInput(0), "Deploy target with id \"0\" does not exist"
            },
            // (2) Merge deploy targets
            {
                new MergeInput("/platform/platform-v1-2.json"), null
            },
// @formatter:on
        });
    }

    protected ConfigurationParser getConfigurationParser() {
        return new ConfigurationParser();
    }

    protected com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao getDeployTargetDao() {
        DeployTargetDao dao = new DeployTargetDao();
        dao.emf = EMF;
        return dao;
    }

    private void loadExceptionForInput(Input input) {
        expectedException.expectMessage(exceptionMessage);
        if (input instanceof AddInput) {
            expectedException.expect(ConflictException.class);
        } else if (input instanceof RemoveInput) {
            expectedException.expect(NotFoundException.class);
        }
    }

}
