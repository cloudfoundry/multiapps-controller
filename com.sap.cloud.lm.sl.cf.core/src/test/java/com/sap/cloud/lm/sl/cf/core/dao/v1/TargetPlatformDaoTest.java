package com.sap.cloud.lm.sl.cf.core.dao.v1;

import static com.sap.cloud.lm.sl.common.util.TestUtil.getResourceAsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.dao.TargetPlatformDao;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;

@RunWith(Parameterized.class)
public class TargetPlatformDaoTest {

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
        String targetPlatformName;

        public RemoveInput(String targetPlatformName) {
            this.targetPlatformName = targetPlatformName;
        }

        public String getTargetPlatformName() {
            return this.targetPlatformName;
        }
    }

    protected static class MergeInput extends Input {
        String existingTargetPlatformName;
        String targetPlatformResource;

        public MergeInput(String existingTargetPlatformName, String targetPlatformResource) {
            this.existingTargetPlatformName = existingTargetPlatformName;
            this.targetPlatformResource = targetPlatformResource;
        }

        public String getExistingTargetPlatformName() {
            return existingTargetPlatformName;
        }

        public String getTargetPlatformResource() {
            return targetPlatformResource;
        }

    }

    protected String resource = "/platform/platform-v1.json";
    private String exceptionMessage;

    private Input input;
    private TargetPlatformDao dao = getTargetPlatformDao();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public TargetPlatformDaoTest(Input input, String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
        this.input = input;
    }

    @Before
    public void setUp() throws Exception {
        TargetPlatform tp = loadTargetPlatform(resource);
        dao.add(tp);
    }

    @Test
    public void testAdd() throws Exception {
        assumeTrue(input instanceof AddInput);
        AddInput addInput = (AddInput) input;
        loadExceptionForInput(addInput);

        TargetPlatform tp1 = loadTargetPlatform(addInput.getAdditionalResource());
        dao.add(tp1);

        List<TargetPlatform> platforms = dao.findAll();
        assertEquals(2, platforms.size());

        TargetPlatform tp2 = platforms.get(1);

        assertTargetPlatforms(tp1, tp2);

        dao.add(tp1);
    }

    @Test
    public void testMerge() throws Exception {
        assumeTrue(input instanceof MergeInput);
        MergeInput mergeInput = (MergeInput) input;

        TargetPlatform tp2 = loadTargetPlatform(mergeInput.getTargetPlatformResource());

        dao.merge(mergeInput.getExistingTargetPlatformName(), tp2);

        assertTargetPlatforms(tp2, dao.find(tp2.getName()));
    }

    @Test
    public void testRemove() throws Exception {
        assumeTrue(input instanceof RemoveInput);

        loadExceptionForInput(input);

        RemoveInput removeInput = (RemoveInput) input;
        List<TargetPlatform> platforms = dao.findAll();

        assertEquals(1, platforms.size());
        dao.remove(removeInput.getTargetPlatformName());
        assertEquals(0, dao.findAll().size());

        dao.remove("TARGET-PLATFORM-TEST");
    }

    @After
    public void clearDatabase() throws Exception {
        for (TargetPlatform tp : dao.findAll()) {
            dao.remove(tp.getName());
        }
    }

    protected void assertTargetPlatforms(TargetPlatform tp1, TargetPlatform tp2) {
        String tp1Json = JsonUtil.toJson(tp1, true);
        String tp2Json = JsonUtil.toJson(tp2, true);
        System.out.println(tp2Json);

        assertEquals(tp1Json, tp2Json);
    }

    protected TargetPlatform loadTargetPlatform(String targetPlatformJsonPath) throws Exception {
        String json = getResourceAsString(targetPlatformJsonPath, getClass());
        return getConfigurationParser().parsePlatformJson(json);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Add new target platform:
            {
                 new AddInput("/platform/platform-v1-1.json"), "Target platform with name \"TARGET-PLATFORM-TEST-1\" already exists"
            },
            // (1) Remove target platform:
            {
                 new RemoveInput("TARGET-PLATFORM-TEST"), "Target platform with name \"TARGET-PLATFORM-TEST\" does not exist"
            },
            // (2) Merge target platforms
            {
                new MergeInput("TARGET-PLATFORM-TEST", "/platform/platform-v1-2.json"), null
            },
// @formatter:on
        });
    }

    protected ConfigurationParser getConfigurationParser() {
        return new ConfigurationParser();
    }

    protected TargetPlatformDao getTargetPlatformDao() {
        com.sap.cloud.lm.sl.cf.core.dao.v1.TargetPlatformDao dao = new com.sap.cloud.lm.sl.cf.core.dao.v1.TargetPlatformDao();
        dao.emf = Persistence.createEntityManagerFactory("TargetPlatformManagement");
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
