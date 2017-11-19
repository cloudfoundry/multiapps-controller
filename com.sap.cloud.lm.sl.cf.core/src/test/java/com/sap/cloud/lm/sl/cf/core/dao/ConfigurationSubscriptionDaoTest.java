package com.sap.cloud.lm.sl.cf.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ModuleDto;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription.ResourceDto;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestCase;
import com.sap.cloud.lm.sl.common.util.TestInput;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.JsonSerializationOptions;

@RunWith(Enclosed.class)
public class ConfigurationSubscriptionDaoTest {

    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("ConfigurationSubscriptionManagement");

    @RunWith(Parameterized.class)
    public static class ConfigurationSubscriptionDaoTest1 {

        private static final String DATABASE_CONTENT_LOCATION = "configuration-subscription-content.json";

        private ConfigurationSubscriptionDao dao = getDao();
        private TestCase<TestInput> test;

        public ConfigurationSubscriptionDaoTest1(TestCase<TestInput> test) {
            this.test = test;
        }

        @Parameters
        public static List<Object[]> getParameters() throws Exception {
            return Arrays.asList(new Object[][] {
    // @formatter:off
                // (00) Add non-existing subscription:
                {
                    new AddTest(new AddTestInput("configuration-subscription-04.json"), "R:configuration-subscription-04.json"),
                },
                // (01) Add subscription that violates the unique constraint:
                {
                    new AddTest(new AddTestInput("configuration-subscription-00.json"), "E:Configuration subscription for MTA \"com.sap.sample.mta.framework\", app \"framework\" and resource \"plugin\" already exists in space \"sap\""),
                },
                // (02) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-09.json"), "E:Configuration subscription's \"resource_name\" column value should not be null"),
                },
                // (03) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-08.json"), "E:Configuration subscription's \"module\" column value should not be null"),
                },
                // (04) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-06.json"), "E:Configuration subscription's \"mta_id\" column value should not be null"),
                },
                // (05) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-05.json"), "E:Configuration subscription's \"filter\" column value should not be null"),
                },
                // (06) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-07.json"), "E:Configuration subscription's \"app_name\" column value should not be null"),
                },
                // (07) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-10.json"), "E:Configuration subscription's \"space_id\" column value should not be null"),
                },
                // (08) Delete exiting subscription:
                {
                    new RemoveTest(new RemoveTestInput("com.sap.sample.mta.framework", "framework", "sap", "plugin"), "R:configuration-subscription-00.json"),
                },
                // (09) Find all subscriptions for certain entries:
                {
                    new FindAllTest(new FindAllTestInput("configuration-entries-00.json"),  "R:configuration-subscription-dao-test-output-00.json"),
                },
                // (10) Find all subscriptions for certain entries:
                {
                    new FindAllTest(new FindAllTestInput("configuration-entries-01.json"),  "R:configuration-subscription-dao-test-output-01.json"),
                },
                // (11) Update existing subscription:
                {
                    new UpdateTest(new UpdateTestInput("com.sap.sample.mta.framework", "framework", "sap", "plugin", "configuration-subscription-00-updated-01.json"), "E:Configuration subscription for MTA \"com.sap.sample.mta.test-1\", app \"test-1\" and resource \"test\" already exists in space \"sap\""),
                },
                // (12) Update existing subscription:
                {
                    new UpdateTest(new UpdateTestInput("com.sap.sample.mta.framework", "framework", "sap", "plugin", "configuration-subscription-00-updated-00.json"), "R:configuration-subscription-00-updated-00.json"),
                },
    // @formatter:on
            });
        }

        @Before
        public void insertDatabaseEntries() throws Exception {
            Type type = new TypeToken<List<ConfigurationSubscription>>() {
            }.getType();

            List<ConfigurationSubscription> subscriptions = JsonUtil.convertJsonToList(
                TestUtil.getResourceAsString(DATABASE_CONTENT_LOCATION, getClass()), type);

            for (ConfigurationSubscription subscription : subscriptions) {
                dao.add(subscription);
            }
        }

        @After
        public void deleteDatabaseEntries() throws Exception {
            for (ConfigurationSubscription entry : dao.findAll()) {
                dao.remove(entry.getId());
            }
        }

        @Test
        public void test() throws Exception {
            test.run();
        }

        private static class FindOneTestInput extends TestInput {

            public String spaceId;
            public String appName;
            public String resourceName;
            public String mtaId;

            public FindOneTestInput(String mtaId, String appName, String spaceId, String resourceName) {
                this.spaceId = spaceId;
                this.appName = appName;
                this.resourceName = resourceName;
                this.mtaId = mtaId;
            }

        }

        private static class FindAllTestInput extends TestInput {

            public List<ConfigurationEntry> entries;

            public FindAllTestInput(String entriesLocation) throws Exception {
                this.entries = loadJsonInput(entriesLocation, new TypeToken<List<ConfigurationEntry>>() {
                }.getType(), getClass());
            }

        }

        private static class UpdateTestInput extends FindOneTestInput {

            public ConfigurationSubscription subscription;

            public UpdateTestInput(String mtaId, String appName, String spaceId, String resourceName, String subscriptionLocation)
                throws Exception {
                super(mtaId, appName, spaceId, resourceName);
                this.subscription = loadJsonInput(subscriptionLocation, ConfigurationSubscription.class, getClass());
            }

        }

        private static class AddTestInput extends TestInput {

            public ConfigurationSubscription subscription;

            public AddTestInput(String subscriptionLocation) throws Exception {
                this.subscription = loadJsonInput(subscriptionLocation, ConfigurationSubscription.class, getClass());
            }

        }

        private static class RemoveTestInput extends FindOneTestInput {

            public RemoveTestInput(String mtaId, String appName, String spaceId, String resourceName) throws Exception {
                super(mtaId, appName, spaceId, resourceName);
            }

        }

        private static class RemoveTest extends TestCase<RemoveTestInput> {

            public RemoveTest(RemoveTestInput input, String expected) {
                super(input, expected);
            }

            private ConfigurationSubscriptionDao dao = getDao();

            @Override
            protected void test() throws Exception {
                TestUtil.test(() -> dao.remove(findOne(input, dao).getId()), expected, getClass(),
                    new JsonSerializationOptions(true, false));
            }

        }

        private static class FindAllTest extends TestCase<FindAllTestInput> {

            public FindAllTest(FindAllTestInput input, String expected) {
                super(input, expected);
            }

            @Override
            protected void test() throws Exception {
                TestUtil.test(() -> findAll(input, getDao()), expected, getClass(), new JsonSerializationOptions(true, false));
            }

        }

        private static class UpdateTest extends TestCase<UpdateTestInput> {

            public UpdateTest(UpdateTestInput input, String expected) {
                super(input, expected);
            }

            private ConfigurationSubscriptionDao dao = getDao();

            @Override
            protected void test() throws Exception {
                TestUtil.test(() -> dao.update(findOne(input, dao).getId(), input.subscription), expected, getClass(),
                    new JsonSerializationOptions(true, false));
            }

        }

        private static class AddTest extends TestCase<AddTestInput> {

            public AddTest(AddTestInput input, String expected) {
                super(input, expected);
            }

            @Override
            protected void test() {
                TestUtil.test(() -> getDao().add(input.subscription), expected, getClass(), new JsonSerializationOptions(true, false));
            }

        }

        private static List<ConfigurationSubscription> findAll(FindAllTestInput input, ConfigurationSubscriptionDao dao) {
            return dao.findAll(input.entries);
        }

        private static ConfigurationSubscription findOne(FindOneTestInput input, ConfigurationSubscriptionDao dao) {
            List<ConfigurationSubscription> subscriptions = dao.findAll(input.mtaId, input.appName, input.spaceId, input.resourceName);
            assertEquals(subscriptions.size(), 1);
            return subscriptions.get(0);
        }

    }

    public static class ConfigurationSubscriptionDaoTest2 {

        @Test
        public void testFind() {
            try {
                getDao().find(1);
                fail();
            } catch (NotFoundException e) {
                assertEquals(MessageFormat.format(Messages.CONFIGURATION_SUBSCRIPTION_NOT_FOUND, 1), e.getMessage());
            }
        }

        @Test
        public void testUpdate() {
            try {
                ResourceDto resourceDto = new ResourceDto("", Collections.emptyMap());
                ModuleDto moduleDto = new ModuleDto("", Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
                getDao().update(1, new ConfigurationSubscription(0, "", "", "", new ConfigurationFilter(null, null, null, null, null),
                    moduleDto, resourceDto));
                fail();
            } catch (SLException e) {
                assertEquals(MessageFormat.format(Messages.CONFIGURATION_SUBSCRIPTION_NOT_FOUND, 1), e.getMessage());
            }
        }

        @Test
        public void testRemove() {
            try {
                getDao().remove(1);
                fail();
            } catch (NotFoundException e) {
                assertEquals(MessageFormat.format(Messages.CONFIGURATION_SUBSCRIPTION_NOT_FOUND, 1), e.getMessage());
            }
        }

    }

    private static ConfigurationSubscriptionDao getDao() {
        ConfigurationSubscriptionDtoDao dtoDao = new ConfigurationSubscriptionDtoDao();
        dtoDao.emf = EMF;
        ConfigurationSubscriptionDao dao = new ConfigurationSubscriptionDao();
        dao.dao = dtoDao;
        return dao;
    }

}
