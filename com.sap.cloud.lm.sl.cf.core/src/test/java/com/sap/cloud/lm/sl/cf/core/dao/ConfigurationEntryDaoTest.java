package com.sap.cloud.lm.sl.cf.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestCase;
import com.sap.cloud.lm.sl.common.util.TestInput;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.JsonSerializationOptions;
import com.sap.cloud.lm.sl.mta.model.Version;

@RunWith(Enclosed.class)
public class ConfigurationEntryDaoTest {

    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("ConfigurationEntryManagement");

    @RunWith(Parameterized.class)
    public static class ConfigurationEntryDaoParameterizedTest {

        private static final String DATABASE_CONTENT_LOCATION = "configuration-registry-content.json";

        private ConfigurationEntryDao dao = createDao();

        private TestCase<TestInput> test;

        public ConfigurationEntryDaoParameterizedTest(TestCase<TestInput> test) {
            this.test = test;
        }

        @Parameters
        public static List<Object[]> getParameters() throws Exception {
            return Arrays.asList(new Object[][] {
    // @formatter:off
                // (0)
                {
                    new AddTest(new AddTestInput("configuration-entry-07.json"), "R:configuration-entry-07.json"),
                },
                // (1)
                {
                    new AddTest(new AddTestInput("configuration-entry-01.json"), "E:Configuration entry with namespace ID \"n-1\", ID \"id-1\", version \"1.0.0\" and target space \"s-1\" already exists"),
                },
                // (2)
                {
                    new FindTest(new FindTestInput("n-1", "id-1", "1.0.0", "s-1", null, null), "R:configuration-entry-01.json"),
                },
                // (3)
                {
                    new RemoveTest(new RemoveTestInput("n-1", "id-1", "1.0.0", "s-1", null), ""),
                },
                // (4)
                {
                    new FindAllTest(new FindTestInput(null, "id-1", "1.0.0", null, null, null), "R:configuration-entry-dao-test-output-04.json"),
                },
                // (5)
                {
                    new FindAllTest(new FindTestInput("n-3", "id-3", "3.0.0", "s-3", null, null), "R:configuration-entry-dao-test-output-05.json"),
                },
                // (6)
                {
                    new FindAllTest(new FindTestInput("n-1", null, null, "s-1", null, null), "R:configuration-entry-dao-test-output-06.json"),
                },
                // (7)
                {
                    new AddTest(new AddTestInput("configuration-entry-08.json"), "R:configuration-entry-08.json"),
                },
                // (8)
                {
                    new FindAllTest(new FindTestInput(null, null, null, null, null, null), "R:configuration-registry-content.json"),
                },
                // (9)
                {
                    new FindAllTest(new FindTestInput(null, null, "> 1.0.0", null, null, null), "R:configuration-entry-dao-test-output-09.json"),
                },
                // (10)
                {
                    new FindAllTest(new FindTestInput(null, null, "= 1.0.0", null, null, null), "R:configuration-entry-dao-test-output-10.json"),
                },
                // (11)
                {
                    new FindAllTest(new FindTestInput(null, null, ">=1.0.0", null, null, null), "R:configuration-entry-dao-test-output-11.json"),
                },
                // (12)
                {
                    new FindAllTest(new FindTestInput(null, null, ">=1.0.0", null, MapUtil.asMap("type", "test"), null), "R:configuration-entry-dao-test-output-12.json"),
                },
                // (13)
                {
                    new RemoveTest(new RemoveTestInput(null, "id-2", null, "s-2", null), ""),
                },
                // (14)
                {
                    new FindTest(new FindTestInput(null, "id-6", null, "s-6", null, null), "R:configuration-entry-06.json"),
                },
                // (15)
                {
                    new UpdateTest(new UpdateTestInput("n-1", "id-1", "1.0.0", "s-1", "configuration-entry-dao-test-input-18.json"), "R:configuration-entry-dao-test-output-15.json"),
                },
                // (16)
                {
                    new UpdateTest(new UpdateTestInput("n-1", "id-1", "1.0.0", "s-1", "configuration-entry-dao-test-input-19.json"), "R:configuration-entry-dao-test-output-16.json"),
                },
                // (17)
                {
                    new UpdateTest(new UpdateTestInput("n-1", "id-1", "1.0.0", "s-1", "configuration-entry-dao-test-input-20.json"), "R:configuration-entry-dao-test-output-17.json"),
                },
                // (18)
                {
                    new UpdateTest(new UpdateTestInput("n-1", "id-1", "1.0.0", "s-1", "configuration-entry-dao-test-input-21.json"), "E:Configuration entry with namespace ID \"n-2\", ID \"id-2\", version \"2.0.0\" and target space \"s-2\" already exists"),
                },
                // (19)
                {
                    new FindAllTest(new FindTestInput(null, null, null, null, null, "com.sap.example.mta"), "R:configuration-entry-dao-test-output-19.json"),
                },
    // @formatter:on
            });
        }

        @Before
        public void prepare() throws Exception {
            Type type = new TypeToken<List<ConfigurationEntry>>() {
            }.getType();

            List<ConfigurationEntry> entries = JsonUtil.convertJsonToList(
                TestUtil.getResourceAsString(DATABASE_CONTENT_LOCATION, getClass()), type);
            for (ConfigurationEntry entry : entries) {
                dao.add(entry);
            }
        }

        @After
        public void clearDatabase() throws Exception {
            for (ConfigurationEntry entry : dao.findAll()) {
                dao.remove(entry.getId());
            }
        }

        @Test
        public void test() throws Exception {
            test.run();
        }

        private static class RemoveTestInput extends FindTestInput {

            public RemoveTestInput(String nid, String id, String version, String target, Map<String, String> requiredProperties) {
                super(nid, id, version, target, requiredProperties, null);
            }

        }

        private static class UpdateTestInput extends FindTestInput {

            public ConfigurationEntry configurationEntry;

            public UpdateTestInput(String nid, String id, String version, String target, String configurationEntryLocation)
                throws Exception {
                super(nid, id, version, target, Collections.emptyMap(), null);
                configurationEntry = TestInput.loadJsonInput(configurationEntryLocation, ConfigurationEntry.class, getClass());
            }

        }

        private static class FindTestInput extends TestInput {

            public String nid, id, version, target, mtaId;
            public Map<String, String> requiredProperties;

            public FindTestInput(String nid, String id, String version, String target, Map<String, String> requiredProperties,
                String mtaId) {
                this.version = version;
                this.nid = nid;
                this.target = target;
                this.id = id;
                this.requiredProperties = requiredProperties;
                this.mtaId = mtaId;
            }

        }

        private static class AddTestInput extends TestInput {

            public ConfigurationEntry configurationEntry;

            public AddTestInput(String configurationEntryLocation) throws Exception {
                configurationEntry = TestInput.loadJsonInput(configurationEntryLocation, ConfigurationEntry.class, getClass());
            }

        }

        private static class RemoveTest extends TestCase<RemoveTestInput> {

            ConfigurationEntryDao dao = createDao();

            public RemoveTest(RemoveTestInput input, String expected) {
                super(input, expected);
            }

            @Override
            public void test() {
                TestUtil.test(() -> {

                    ConfigurationEntry entry = findConfigurationEntries(input, dao).get(0);
                    dao.remove(entry.getId());
                    assertTrue(!dao.exists(entry.getId()));

                } , expected);
            }

        }

        private static class UpdateTest extends TestCase<UpdateTestInput> {

            ConfigurationEntryDao dao = createDao();

            public UpdateTest(UpdateTestInput input, String expected) {
                super(input, expected);
            }

            @Override
            protected void test() throws Exception {
                TestUtil.test(() -> {

                    return dao.update(findConfigurationEntries(input, dao).get(0).getId(), input.configurationEntry);

                } , expected, getClass(), new JsonSerializationOptions(true, false));
            }

        }

        private static class AddTest extends TestCase<AddTestInput> {

            public AddTest(AddTestInput input, String expected) {
                super(input, expected);
            }

            ConfigurationEntryDao dao = createDao();

            @Override
            public void test() {
                TestUtil.test(() -> {

                    return dao.add(input.configurationEntry);

                } , expected, getClass(), new JsonSerializationOptions(true, false));
            }

        }

        private static class FindTest extends TestCase<FindTestInput> {

            public FindTest(FindTestInput input, String expected) {
                super(input, expected);
            }

            ConfigurationEntryDao dao = createDao();

            @Override
            public void test() {
                TestUtil.test(() -> {

                    return dao.find(findConfigurationEntries(input, dao).get(0).getId());

                } , expected, getClass(), new JsonSerializationOptions(true, false));
            }

        }

        private static class FindAllTest extends TestCase<FindTestInput> {

            public FindAllTest(FindTestInput input, String expected) {
                super(input, expected);
            }

            @Override
            public void test() {
                TestUtil.test(() -> {

                    return findConfigurationEntries(input, createDao());

                } , expected, getClass(), new JsonSerializationOptions(true, false));
            }

        }

        private static List<ConfigurationEntry> findConfigurationEntries(FindTestInput input, ConfigurationEntryDao dao) {
            return dao.find(input.nid, input.id, input.version, input.target, input.requiredProperties, input.mtaId);
        }

    }

    public static class ConfigurationEntryDaoStandardTest {

        private ConfigurationEntryDao dao = createDao();

        @Test
        public void testFind() {
            long id = getUnusedId(dao);
            try {
                dao.find(id);
                fail();
            } catch (NotFoundException e) {
                assertEquals(MessageFormat.format(Messages.CONFIGURATION_ENTRY_NOT_FOUND, id), e.getMessage());
            }
        }

        @Test
        public void testUpdate() {
            long id = getUnusedId(dao);
            try {
                dao.update(id, new ConfigurationEntry("", "", Version.parseVersion("1.0.0"), "", ""));
                fail();
            } catch (SLException e) {
                assertEquals(MessageFormat.format(Messages.CONFIGURATION_ENTRY_NOT_FOUND, id), e.getMessage());
            }
        }

        @Test
        public void testRemove() {
            long id = getUnusedId(dao);
            try {
                dao.remove(id);
                fail();
            } catch (NotFoundException e) {
                assertEquals(MessageFormat.format(Messages.CONFIGURATION_ENTRY_NOT_FOUND, id), e.getMessage());
            }
        }

        private long getUnusedId(ConfigurationEntryDao dao) {
            for (long id = 0; id <= Long.MAX_VALUE; id++) {
                boolean isUsed = dao.exists(id);
                if (!isUsed) {
                    return id;
                }
            }
            throw new UnsupportedOperationException();
        }

    }

    private static ConfigurationEntryDao createDao() {
        ConfigurationEntryDtoDao dtoDao = new ConfigurationEntryDtoDao();
        dtoDao.emf = EMF;
        ConfigurationEntryDao dao = new ConfigurationEntryDao();
        dao.dao = dtoDao;
        return dao;
    }

}
