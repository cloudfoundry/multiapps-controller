package org.cloudfoundry.multiapps.controller.web.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ActiveOperationsJmxReporterTest {

    @Mock
    private OperationService operationService;

    private MBeanServer mBeanServer;
    private ActiveOperationsJmxReporter reporter;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        mBeanServer = MBeanServerFactory.createMBeanServer();
        reporter = new ActiveOperationsJmxReporter(operationService, mBeanServer);
    }

    @AfterEach
    void tearDown() throws Exception {
        MBeanServerFactory.releaseMBeanServer(mBeanServer);
        mocks.close();
    }

    @Test
    void testRegistersMBeansPerUserAndSpace() throws Exception {
        stubOperations(List.of(operation("alice@sap.com", "space-1"),
                               operation("alice@sap.com", "space-2"),
                               operation("bob@sap.com", "space-1")));

        reporter.refresh();

        assertEquals(2L, getCount(ActiveOperationsJmxReporter.USER_OBJECT_NAME_PATTERN, ActiveOperationsJmxReporter.hashUser("alice@sap.com")));
        assertEquals(1L, getCount(ActiveOperationsJmxReporter.USER_OBJECT_NAME_PATTERN, ActiveOperationsJmxReporter.hashUser("bob@sap.com")));
        assertEquals(2L, getCount(ActiveOperationsJmxReporter.SPACE_OBJECT_NAME_PATTERN, "space-1"));
        assertEquals(1L, getCount(ActiveOperationsJmxReporter.SPACE_OBJECT_NAME_PATTERN, "space-2"));
    }

    @Test
    void testUnregistersMBeanWhenUserHasNoMoreActiveOperations() throws Exception {
        stubOperations(List.of(operation("alice@sap.com", "space-1")));
        reporter.refresh();

        stubOperations(List.of());
        reporter.refresh();

        ObjectName name = new ObjectName(ActiveOperationsJmxReporter.USER_OBJECT_NAME_PATTERN.formatted(ObjectName.quote(ActiveOperationsJmxReporter.hashUser("alice@sap.com"))));
        assertEquals(false, mBeanServer.isRegistered(name));
    }

    @Test
    void testUpdatesCountWhenOperationsChange() throws Exception {
        stubOperations(List.of(operation("alice@sap.com", "space-1")));
        reporter.refresh();
        assertEquals(1L, getCount(ActiveOperationsJmxReporter.USER_OBJECT_NAME_PATTERN, ActiveOperationsJmxReporter.hashUser("alice@sap.com")));

        stubOperations(List.of(operation("alice@sap.com", "space-1"),
                               operation("alice@sap.com", "space-2")));
        reporter.refresh();
        assertEquals(2L, getCount(ActiveOperationsJmxReporter.USER_OBJECT_NAME_PATTERN, ActiveOperationsJmxReporter.hashUser("alice@sap.com")));
    }

    private void stubOperations(List<Operation> operations) {
        OperationQuery query = mock(OperationQuery.class);
        when(operationService.createQuery()).thenReturn(query);
        when(query.inNonFinalState()).thenReturn(query);
        when(query.list()).thenReturn(operations);
    }

    private long getCount(String pattern, String key) throws Exception {
        ObjectName name = new ObjectName(pattern.formatted(ObjectName.quote(key)));
        return (long) mBeanServer.getAttribute(name, "Count");
    }

    private Operation operation(String user, String spaceId) {
        Operation op = mock(Operation.class);
        when(op.getUser()).thenReturn(user);
        when(op.getSpaceId()).thenReturn(spaceId);
        return op;
    }

}
