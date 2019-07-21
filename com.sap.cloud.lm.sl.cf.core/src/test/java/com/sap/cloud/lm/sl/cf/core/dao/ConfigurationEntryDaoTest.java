package com.sap.cloud.lm.sl.cf.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestCase;
import com.sap.cloud.lm.sl.common.util.TestInput;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.model.Version;

@RunWith(Enclosed.class)
public class ConfigurationEntryDaoTest {

    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("TestDefault");

    private static final Tester TESTER = Tester.forClass(ConfigurationEntryDaoTest.class);

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
                    new AddTest(new AddTestInput("configuration-entry-07.json"),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-07.json")),
                },
                // (1)
                {
                    new AddTest(new AddTestInput("configuration-entry-01.json"),
                        new Expectation(Expectation.Type.EXCEPTION, "Configuration entry with namespace ID \"n-1\", ID \"id-1\", version \"1.0.0\", target org \"org-1\" and target space \"space-1\", already exists")),
                },
                // (2)
                {
                    new FindTest(new FindTestInput("n-1", "id-1", "1.0.0", new CloudTarget("org-1", "space-1"), null, null, null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-01.json")),
                },
                // (3)
                {
                    new RemoveTest(new RemoveTestInput("n-1", "id-1", "1.0.0", new CloudTarget("org-1", "space-1"), null, null, ""),
                        new Expectation(null)),
                },
                // (4)
                {
                    new FindAllTest(new FindTestInput(null, "id-1", "1.0.0", new CloudTarget("org-1", "space-1"), null, null, null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-04.json")),
                },
                // (5)
                {
                    new FindAllTest(new FindTestInput("n-3", "id-3", "3.0.0", new CloudTarget("org-3", "space-3"), null, null, null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-05.json")),
                },
                // (6)
                {
                    new FindAllTest(new FindTestInput("n-1", null, null, new CloudTarget("org-1", "space-1"), null, null, null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-06.json")),
                },
                // (7)
                {
                    new AddTest(new AddTestInput("configuration-entry-08.json"),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-08.json")),
                },
                // (8)
                {
                    new FindAllTest(new FindTestInput(null, null, null, null, null, null, null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-registry-content.json")),
                },
                // (9)
                {
                    new FindAllTest(new FindTestInput(null, null, "> 1.0.0", null, null, null, null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-09.json")),
                },
                // (10)
                {
                    new FindAllTest(new FindTestInput(null, null, "= 1.0.0", null, null, null, null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-10.json")),
                },
                // (11)
                {
                    new FindAllTest(new FindTestInput(null, null, ">=1.0.0", null, null, null, null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-11.json")),
                },
                // (12)
                {
                    new FindAllTest(new FindTestInput(null, null, ">=1.0.0", null, MapUtil.asMap("type", "test"), null, null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-12.json")),
                },
                // (13)
                {
                    new RemoveTest(new RemoveTestInput(null, "id-2", null, new CloudTarget("org-2", "space-2"), null, null, ""),
                        new Expectation(null)),
                },
                // (14)
                {
                    new FindTest(new FindTestInput(null, "id-6", null, new CloudTarget("org-6", "space-6"), null, null, null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-06.json")),
                },
                // (15)
                {
                    new UpdateTest(new UpdateTestInput("n-1", "id-1", "1.0.0", new CloudTarget("org-1", "space-1"), "configuration-entry-dao-test-input-18.json", null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-15.json")),
                },
                // (16)
                {
                    new UpdateTest(new UpdateTestInput("n-1", "id-1", "1.0.0", new CloudTarget("org-1", "space-1"), "configuration-entry-dao-test-input-19.json", null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-16.json")),
                },
                // (17)
                {
                    new UpdateTest(new UpdateTestInput("n-1", "id-1", "1.0.0", new CloudTarget("org-1", "space-1"), "configuration-entry-dao-test-input-20.json", null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-17.json")),
                },
                // (18)
                {
                    new UpdateTest(new UpdateTestInput("n-1", "id-1", "1.0.0", new CloudTarget("org-1", "space-1"), "configuration-entry-dao-test-input-21.json", null, ""),
                        new Expectation(Expectation.Type.EXCEPTION, "Configuration entry with namespace ID \"n-2\", ID \"id-2\", version \"2.0.0\", target org \"org-2\" and target space \"space-2\", already exists")),
                },
                // (19)
                {
                    new FindAllTest(new FindTestInput(null, null, null, null, null, "com.sap.example.mta", null, ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-19.json")),
                },
                // (20)
                {
                    new FindAllTest(new FindTestInput(null, "id-2", null, null, null, null, Arrays.asList(new CloudTarget("org-2", "space-2")), ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-20.json")),
                },              
                // (21)
                {
                    new FindAllTest(new FindTestInput(null, null, "0.1.0", null, null, null, Arrays.asList(new CloudTarget("myorg", "*")), ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-21.json")),
                },
                // (22)
                {
                    new FindAllTest(new FindTestInput(null, null, null, null, MapUtil.asMap("type", "test"), null, Arrays.asList(new CloudTarget("org-3", "space-3")), ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-22.json")),
                },
                // (23)
                {
                    new FindAllTest(new FindTestInput("n-2", null, null, null, null, null, Arrays.asList(new CloudTarget("*", "")), ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-23.json")),
                },
                // (24)
                {
                    new FindAllTest(new FindTestInput(null, null, null, null, MapUtil.asMap("type", "test"), null, Arrays.asList(new CloudTarget("org-3", "space-3"), new CloudTarget("org-4", "space-4")), ""),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-24.json")),
                }, 
                // (25)               
                {
                    new FindAllTest(new FindTestInput(null, "s-2", null, null, null, null, Arrays.asList(new CloudTarget("myorg1", "myspace1")), ""),
                        new Expectation("[]")),
                },
                // (26)
                {
                    new FindAllGuidTest(new FindTestInput(null, null, null, null, null, null, null, "fbd3dc79-1a54-4a70-8022-ab716643809b"),
                        new Expectation(Expectation.Type.JSON, "configuration-entry-dao-test-output-26.json")),
                }
    // @formatter:on
            });
        }

        @Before
        public void prepare() throws Exception {
            List<ConfigurationEntry> entries = JsonUtil.convertJsonToList(
                TestUtil.getResourceAsString(DATABASE_CONTENT_LOCATION, getClass()), new TypeReference<List<ConfigurationEntry>>() {
                });

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

            public RemoveTestInput(String nid, String id, String version, CloudTarget target, Map<String, Object> requiredProperties,
                List<CloudTarget> cloudTargets, String spaceId) {
                super(nid, id, version, target, requiredProperties, null, cloudTargets, spaceId);
            }

        }

        private static class UpdateTestInput extends FindTestInput {

            public ConfigurationEntry configurationEntry;

            public UpdateTestInput(String nid, String id, String version, CloudTarget target, String configurationEntryLocation,
                List<CloudTarget> cloudTarget, String spaceId) throws Exception {
                super(nid, id, version, target, Collections.emptyMap(), null, cloudTarget, spaceId);
                configurationEntry = TestInput.loadJsonInput(configurationEntryLocation, ConfigurationEntry.class, getClass());
            }

        }

        private static class FindTestInput extends TestInput {

            public String nid, id, version, mtaId, spaceId;
            public CloudTarget target;
            public Map<String, Object> requiredProperties;
            public List<CloudTarget> cloudTargets;

            public FindTestInput(String nid, String id, String version, CloudTarget target, Map<String, Object> requiredProperties,
                String mtaId, List<CloudTarget> cloudTargets, String spaceId) {
                this.version = version;
                this.nid = nid;
                this.target = target;
                this.id = id;
                this.requiredProperties = requiredProperties;
                this.mtaId = mtaId;
                this.cloudTargets = cloudTargets;
                this.spaceId = spaceId;
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

            public RemoveTest(RemoveTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            @Override
            public void test() {
                TESTER.test(() -> {

                    ConfigurationEntry entry = findConfigurationEntries(input, dao).get(0);
                    dao.remove(entry.getId());
                    assertTrue(!dao.exists(entry.getId()));

                }, expectation);
            }

        }

        private static class UpdateTest extends TestCase<UpdateTestInput> {

            ConfigurationEntryDao dao = createDao();

            public UpdateTest(UpdateTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            @Override
            protected void test() throws Exception {
                TESTER.test(() -> {

                    return removeId(dao.update(findConfigurationEntries(input, dao).get(0)
                        .getId(), input.configurationEntry));

                }, expectation);
            }

        }

        private static class AddTest extends TestCase<AddTestInput> {

            public AddTest(AddTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            ConfigurationEntryDao dao = createDao();

            @Override
            public void test() {
                TESTER.test(() -> {

                    dao.add(input.configurationEntry);
                    return removeId(input.configurationEntry);

                }, expectation);
            }

        }

        private static class FindTest extends TestCase<FindTestInput> {

            public FindTest(FindTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            ConfigurationEntryDao dao = createDao();

            @Override
            public void test() {
                TESTER.test(() -> {

                    long id = findConfigurationEntries(input, dao).get(0)
                        .getId();
                    return removeId(dao.find(id));

                }, expectation);
            }

        }

        private static class FindAllTest extends TestCase<FindTestInput> {

            public FindAllTest(FindTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            @Override
            public void test() {
                TESTER.test(() -> {

                    return removeIds(findConfigurationEntries(input, createDao()));

                }, expectation);
            }
        }

        private static class FindAllGuidTest extends TestCase<FindTestInput> {

            public FindAllGuidTest(FindTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            @Override
            public void test() {
                TESTER.test(() -> {

                    return removeIds(findConfigurationEntriesGuid(input, createDao()));

                }, expectation);
            }
        }

        private static List<ConfigurationEntry> findConfigurationEntries(FindTestInput input, ConfigurationEntryDao dao) {
            return dao.find(input.nid, input.id, input.version, input.target, input.requiredProperties, input.mtaId, input.cloudTargets);
        }

        private static List<ConfigurationEntry> findConfigurationEntriesGuid(FindTestInput input, ConfigurationEntryDao dao) {
            return dao.find(input.spaceId);
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
                dao.update(id, new ConfigurationEntry("", "", Version.parseVersion("1.0.0"), new CloudTarget("", ""), "", null, ""));
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

    private static List<ConfigurationEntry> removeIds(List<ConfigurationEntry> entries) {
        return entries.stream()
            .map(ConfigurationEntryDaoTest::removeId)
            .collect(Collectors.toList());
    }

    private static ConfigurationEntry removeId(ConfigurationEntry entry) {
        entry.setId(0);
        return entry;
    }

    private static ConfigurationEntryDao createDao() {
        ConfigurationEntryDtoDao dtoDao = new ConfigurationEntryDtoDao(EMF);
        ConfigurationEntryDao dao = new ConfigurationEntryDao();
        dao.dao = dtoDao;
        return dao;
    }

}
