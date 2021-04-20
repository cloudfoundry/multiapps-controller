package org.cloudfoundry.multiapps.controller.core.auditlogging.impl;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.logging.log4j.core.Logger;
import org.cloudfoundry.multiapps.controller.core.auditlogging.UserInfoProvider;
import org.cloudfoundry.multiapps.controller.persistence.test.TestDataSourceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import javax.sql.DataSource;

class AuditLogManagerTest {

    private DataSource testDataSource;

    private AuditLogManager auditLogManager;

    private static final String AUDIT_LOG_CHANGELOG_LOCATION = "org/cloudfoundry/multiapps/controller/persistence/db/changelog/db-changelog.xml";

    @BeforeEach
    void setUp() throws Exception {
        testDataSource = TestDataSourceProvider.getDataSource(AUDIT_LOG_CHANGELOG_LOCATION);
        auditLogManager = new AuditLogManager(testDataSource, createTestUserInfoProvider());
    }

    @AfterEach
    void tearDown() throws Exception {
        testDataSource.getConnection()
                      .close();
    }

    @Test
    void testAuditLogManager() {
        List<Logger> loggers = loadAuditLoggers();

        logMessage(loggers);

        assertNull(auditLogManager.getException());
    }

    private List<Logger> loadAuditLoggers() {
        return List.of(auditLogManager.getSecurityLogger(), auditLogManager.getActionLogger(), auditLogManager.getConfigLogger());
    }

    private void logMessage(List<Logger> loggers) {
        loggers.forEach(logger -> logger.info("Test Message"));
    }

    private static UserInfoProvider createTestUserInfoProvider() {
        return () -> null;
    }

}
