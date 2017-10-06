package com.sap.cloud.lm.sl.cf.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
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
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ContextExtension;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestCase;
import com.sap.cloud.lm.sl.common.util.TestInput;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.JsonSerializationOptions;

@RunWith(Enclosed.class)
public class ContextExtensionDaoTest {

    private static final int NON_EXISTING_EXTENSION_CONTEXT_ID = 0;
    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("ContextExtensionManagement");

    @RunWith(Parameterized.class)
    public static class ContextExtensionDaoParameterizedTest {
        private static final String DATABASE_CONTENT = "context-extension-content.json";

        private ContextExtensionDao dao = createDao();

        private TestCase<TestInput> test;

        public ContextExtensionDaoParameterizedTest(TestCase<TestInput> test) {
            this.test = test;
        }

        @Parameters
        public static List<Object[]> getParameters() throws Exception {
            return Arrays.asList(new Object[][] {
    // @formatter:off
                
                //TODO: create the test cases and fix the test
                // (0)
                {
                    new AddTest(new AddTestInput("context-extension-entry-1.json"), "R:context-extension-entry-1.json"),
                },
                // (1)
                {
                    new AddTest(new AddTestInput("context-extension-entry-2.json"), "E:Context extension element with Process ID \"test-process-id\", Name \"test-name\" and Value \"test-value\" already exists"),
                },
                // (3)
                {
                    new RemoveTest(new RemoveTestInput("test-process-id", "test-name", "test"), ""),
                },
                // (4)
                {
                    new FindTest(new FindTestInput("test-process-id", "test-name", "test-value"), "R:context-extension-entry-2.json"),
                },
                // (4)
                {
                    new FindAllProcessIdTest(new FindTestInput("test-process-id", "test-name", "test-value"), "R:context-extension-entry-3.json"),
                },
                // (5)
                {
                    new FindAllTest(new FindTestInput(), "R:context-extension-content.json"),
                },
                // (6)
                {
                    new UpdateTest(new UpdateTestInput("test-process-id", "test-name", "context-extension-entry-4.json"), "R:context-extension-entry-5.json"),
                }
    // @formatter:on
            });

        }

        @Before
        public void prepareDatabase() throws Exception {
            Type type = new TypeToken<List<ContextExtension>>() {
            }.getType();

            List<ContextExtension> entries = JsonUtil.convertJsonToList(TestUtil.getResourceAsString(DATABASE_CONTENT, getClass()), type);
            for (ContextExtension entry : entries) {
                entry.setCreateTime(new Date());
                entry.setLastUpdatedTime(new Date());
                dao.add(entry);
            }
        }

        @After
        public void clearDatabase() throws NotFoundException {
            for (ContextExtension entry : dao.findAll()) {
                dao.remove(entry.getId());
            }
        }

        @Test
        public void test() throws Exception {
            test.run();
        }

        private static class BaseTestInput extends TestInput {
            protected String processId;
            protected String variableKey;

            public BaseTestInput() {
            }

            public BaseTestInput(String processId, String variableKey, String variableValue) {
                this.processId = processId;
                this.variableKey = variableKey;
            }

        }

        private static class RemoveTestInput extends BaseTestInput {

            public RemoveTestInput(String processId, String variableKey, String variableValue) {
                super(processId, variableKey, variableValue);
            }
        }

        private static class RemoveTest extends TestCase<BaseTestInput> {
            ContextExtensionDao dao = createDao();

            public RemoveTest(BaseTestInput input, String expected) {
                super(input, expected);
            }

            @Override
            protected void test() throws Exception {
                TestUtil.test(() -> {

                    ContextExtension entry = findExtensionEntry(input, dao);
                    dao.remove(entry.getId());
                    assertTrue(!dao.exists(entry.getId()));

                } , expected);
            }

        }

        private static class AddTestInput extends TestInput {
            public ContextExtension entry;


            public AddTestInput(String input) throws Exception {
                entry = TestInput.loadJsonInput(input, ContextExtension.class, getClass());
                entry.setCreateTime(new Date());
                entry.setLastUpdatedTime(new Date());
            }
        }

        private static class AddTest extends TestCase<AddTestInput> {
            public AddTest(AddTestInput input, String expected) {
                super(input, expected);
            }

            ContextExtensionDao dao = createDao();

            @Override
            protected void test() throws Exception {
                TestUtil.test(() -> {

                    return dao.add(input.entry);

                } , expected, getClass(), new JsonSerializationOptions(true, false));
            }
        }

        private static class FindTestInput extends BaseTestInput {

            public FindTestInput() {
            }

            public FindTestInput(String processId, String variableKey, String variableValue) {
                super(processId, variableKey, variableValue);
            }
        }

        private static class FindTest extends TestCase<BaseTestInput> {

            public FindTest(FindTestInput input, String expected) {
                super(input, expected);
            }

            ContextExtensionDao dao = createDao();

            @Override
            protected void test() throws Exception {
                TestUtil.test(() -> {
                    ContextExtension entry = findExtensionEntry(input, dao);
                    return dao.find(entry.getId());
                } , expected, getClass(), new JsonSerializationOptions(true, false));
            }
        }

        private static class FindAllProcessIdTest extends TestCase<BaseTestInput> {

            public FindAllProcessIdTest(BaseTestInput input, String expected) {
                super(input, expected);
            }

            ContextExtensionDao dao = createDao();

            @Override
            protected void test() throws Exception {
                TestUtil.test(() -> {
                    return dao.findAll(input.processId);
                } , expected, getClass(), new JsonSerializationOptions(true, false));
            }

        }

        private static class FindAllTest extends TestCase<FindTestInput> {

            public FindAllTest(FindTestInput input, String expected) {
                super(input, expected);
            }

            ContextExtensionDao dao = createDao();

            @Override
            protected void test() throws Exception {
                TestUtil.test(() -> {
                    return dao.findAll();
                } , expected, getClass(), new JsonSerializationOptions(true, false));
            }

        }

        private static class UpdateTestInput extends BaseTestInput {
            public ContextExtension entry;

            public UpdateTestInput(String processId, String name, String contextExtensionEntryLocaltion) throws Exception {
                super(processId, name, null);
                entry = TestInput.loadJsonInput(contextExtensionEntryLocaltion, ContextExtension.class, getClass());
                entry.setCreateTime(new Date());
                entry.setLastUpdatedTime(new Date());
            }

        }

        private static class UpdateTest extends TestCase<UpdateTestInput> {

            public UpdateTest(UpdateTestInput input, String expected) {
                super(input, expected);
            }

            ContextExtensionDao dao = createDao();

            @Override
            protected void test() throws Exception {
                TestUtil.test(() -> {
                    return dao.update(findExtensionEntry(input, dao).getId(), input.entry);
                } , expected, getClass(), new JsonSerializationOptions(true, false));
            }
            
        }

        private static ContextExtension findExtensionEntry(BaseTestInput input, ContextExtensionDao dao) {
            return dao.find(input.processId, input.variableKey);
        }
    }

    public static class ContextExtensionDaoStandardTest {
        private ContextExtensionDao dao = createDao();

        @Test
        public void testFind() {
            try {
                dao.find(NON_EXISTING_EXTENSION_CONTEXT_ID);
                fail();
            } catch (NotFoundException e) {
                assertEquals(MessageFormat.format(Messages.CONTEXT_EXTENSION_ENTRY_NOT_FOUND, 0), e.getMessage());
            }
        }

        @Test
        public void testUpdate() {
            try {
                dao.update(NON_EXISTING_EXTENSION_CONTEXT_ID, new ContextExtension(-1, "", "", "", null, null));
                fail();
            } catch (SLException e) {
                assertEquals(MessageFormat.format(Messages.CONTEXT_EXTENSION_ENTRY_NOT_FOUND, 0), e.getMessage());
            }
        }

        @Test
        public void testRemove() {
            try {
                dao.remove(NON_EXISTING_EXTENSION_CONTEXT_ID);
                fail();
            } catch (NotFoundException e) {
                assertEquals(MessageFormat.format(Messages.CONTEXT_EXTENSION_ENTRY_NOT_FOUND, 0), e.getMessage());
            }
        }
    }

    private static ContextExtensionDao createDao() {
        ContextExtensionDao dao = new ContextExtensionDao();
        dao.emf = EMF;
        return dao;
    }
}
