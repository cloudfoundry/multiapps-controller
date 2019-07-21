package com.sap.cloud.lm.sl.cf.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

@RunWith(Enclosed.class)
public class ConfigurationSubscriptionDaoTest {

    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("TestDefault");
    private static final Tester TESTER = Tester.forClass(ConfigurationEntryDaoTest.class);

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
                    new AddTest(new AddTestInput("configuration-subscription-04.json"),
                        new Expectation(Expectation.Type.JSON, "configuration-subscription-04.json")),
                },
                // (01) Add subscription that violates the unique constraint:
                {
                    new AddTest(new AddTestInput("configuration-subscription-00.json"),
                        new Expectation(Expectation.Type.EXCEPTION, "Configuration subscription for MTA \"com.sap.sample.mta.framework\", app \"framework\" and resource \"plugin\" already exists in space \"sap\"")),
                },
                // (02) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-09.json"),
                        new Expectation(Expectation.Type.EXCEPTION, "Configuration subscription's \"resource_name\" column value should not be null")),
                },
                // (03) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-08.json"),
                        new Expectation(Expectation.Type.EXCEPTION, "Configuration subscription's \"module\" column value should not be null")),
                },
                // (04) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-06.json"),
                        new Expectation(Expectation.Type.EXCEPTION, "Configuration subscription's \"mta_id\" column value should not be null")),
                },
                // (05) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-05.json"),
                        new Expectation(Expectation.Type.EXCEPTION, "Configuration subscription's \"filter\" column value should not be null")),
                },
                // (06) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-07.json"),
                        new Expectation(Expectation.Type.EXCEPTION, "Configuration subscription's \"app_name\" column value should not be null")),
                },
                // (07) Add subscription that has null values:
                {
                    new AddTest(new AddTestInput("configuration-subscription-10.json"),
                        new Expectation(Expectation.Type.EXCEPTION, "Configuration subscription's \"space_id\" column value should not be null")),
                },
                // (08) Delete exiting subscription:
                {
                    new RemoveTest(new RemoveTestInput("com.sap.sample.mta.framework", "framework", "sap", "plugin"),
                        new Expectation(Expectation.Type.JSON, "configuration-subscription-00.json")),
                },
                // (09) Find all subscriptions for certain entries:
                {
                    new FindAllTest(new FindAllTestInput("configuration-entries-00.json"),
                        new Expectation(Expectation.Type.JSON, "configuration-subscription-dao-test-output-00.json")),
                },
                // (10) Find all subscriptions for certain entries:
                {
                    new FindAllTest(new FindAllTestInput("configuration-entries-01.json"),
                        new Expectation(Expectation.Type.JSON, "configuration-subscription-dao-test-output-01.json")),
                },
                // (11) Update existing subscription:
                {
                    new UpdateTest(new UpdateTestInput("com.sap.sample.mta.framework", "framework", "sap", "plugin", "configuration-subscription-00-updated-01.json"),
                        new Expectation(Expectation.Type.EXCEPTION, "Configuration subscription for MTA \"com.sap.sample.mta.test-1\", app \"test-1\" and resource \"test\" already exists in space \"sap\"")),
                },
                // (12) Update existing subscription:
                {
                    new UpdateTest(new UpdateTestInput("com.sap.sample.mta.framework", "framework", "sap", "plugin", "configuration-subscription-00-updated-00.json"),
                        new Expectation(Expectation.Type.JSON, "configuration-subscription-00-updated-00.json")),
                },
                // (13) Find all subscriptions for current guid
                {
                    new FindAllGuidTest(new FindOneTestInput("", "", "fbd3dc79-1a54-4a70-8022-ab716643809b", ""),
                        new Expectation(Expectation.Type.JSON, "configuration-subscription-dao-test-output-13.json")),
                }
    // @formatter:on
            });
        }

        @Before
        public void insertDatabaseEntries() throws Exception {
            List<ConfigurationSubscription> subscriptions = JsonUtil.convertJsonToList(
                TestUtil.getResourceAsString(DATABASE_CONTENT_LOCATION, getClass()), new TypeReference<List<ConfigurationSubscription>>() {
                });

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
                this.entries = loadJsonInput(entriesLocation, new TypeReference<List<ConfigurationEntry>>() {
                }, getClass());
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

            public RemoveTest(RemoveTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            private ConfigurationSubscriptionDao dao = getDao();

            @Override
            protected void test() throws Exception {
                TESTER.test(() -> {
                    ConfigurationSubscription configurationSubscription = findOne(input, dao);
                    dao.remove(configurationSubscription.getId());
                    return removeId(configurationSubscription);
                }, expectation);
            }

        }

        private static class FindAllTest extends TestCase<FindAllTestInput> {

            public FindAllTest(FindAllTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            @Override
            protected void test() throws Exception {
                TESTER.test(() -> removeIds(findAll(input, getDao())), expectation);
            }

        }

        private static class FindAllGuidTest extends TestCase<FindOneTestInput> {

            public FindAllGuidTest(FindOneTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            @Override
            protected void test() throws Exception {
                TESTER.test(() -> removeIds(findAll(input.spaceId, getDao())), expectation);
            }

        }

        private static class UpdateTest extends TestCase<UpdateTestInput> {

            public UpdateTest(UpdateTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            private ConfigurationSubscriptionDao dao = getDao();

            @Override
            protected void test() throws Exception {
                TESTER.test(() -> removeId(dao.update(findOne(input, dao).getId(), input.subscription)), expectation);
            }

        }

        private static class AddTest extends TestCase<AddTestInput> {

            public AddTest(AddTestInput input, Expectation expectation) {
                super(input, expectation);
            }

            @Override
            protected void test() {
                TESTER.test(() -> {
                    getDao().add(input.subscription);
                    return removeId(input.subscription);
                }, expectation);
            }

        }

        private static List<ConfigurationSubscription> findAll(FindAllTestInput input, ConfigurationSubscriptionDao dao) {
            return dao.findAll(input.entries);
        }

        private static List<ConfigurationSubscription> findAll(String guid, ConfigurationSubscriptionDao dao) {
            List<ConfigurationSubscription> subscriptions = dao.findAll(guid);
            return subscriptions;
        }

        private static ConfigurationSubscription findOne(FindOneTestInput input, ConfigurationSubscriptionDao dao) {
            List<ConfigurationSubscription> subscriptions = dao.findAll(input.mtaId, input.appName, input.spaceId, input.resourceName);
            assertEquals(1, subscriptions.size());
            return subscriptions.get(0);
        }

    }

    public static class ConfigurationSubscriptionDaoTest2 {

        @Test
        public void testFind() {
            try {
                getDao().find(1l);
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
                getDao().update(1l, new ConfigurationSubscription(0, "", "", "",
                    new ConfigurationFilter(null, null, null, null, null), moduleDto, resourceDto));
                fail();
            } catch (SLException e) {
                assertEquals(MessageFormat.format(Messages.CONFIGURATION_SUBSCRIPTION_NOT_FOUND, 1), e.getMessage());
            }
        }

        @Test
        public void testRemove() {
            try {
                getDao().remove(1l);
                fail();
            } catch (NotFoundException e) {
                assertEquals(MessageFormat.format(Messages.CONFIGURATION_SUBSCRIPTION_NOT_FOUND, 1), e.getMessage());
            }
        }

    }

    private static List<ConfigurationSubscription> removeIds(List<ConfigurationSubscription> subscriptions) {
        return subscriptions.stream()
            .map(ConfigurationSubscriptionDaoTest::removeId)
            .collect(Collectors.toList());
    }

    private static ConfigurationSubscription removeId(ConfigurationSubscription subscription) {
        subscription.setId(0);
        return subscription;
    }

    private static ConfigurationSubscriptionDao getDao() {
        ConfigurationSubscriptionDao dao = new ConfigurationSubscriptionDao();
        dao.dao = new ConfigurationSubscriptionDtoDao(EMF);
        return dao;
    }

}
