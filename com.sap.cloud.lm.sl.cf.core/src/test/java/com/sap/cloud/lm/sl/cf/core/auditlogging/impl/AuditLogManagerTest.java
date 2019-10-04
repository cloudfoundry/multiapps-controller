package com.sap.cloud.lm.sl.cf.core.auditlogging.impl;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.auditlogging.UserInfoProvider;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.common.util.TestDataSourceProvider;

public class AuditLogManagerTest {

    private DataSource testDataSource;

    private AuditLogManager auditLogManager;

    private static final String AUDIT_LOG_CHANGELOG_LOCATION = "com/sap/cloud/lm/sl/cf/core/db/changelog/db-changelog.xml";

    @BeforeEach
    public void setUp() throws Exception {
        testDataSource = TestDataSourceProvider.getDataSource(AUDIT_LOG_CHANGELOG_LOCATION);
        auditLogManager = new AuditLogManager(testDataSource, createTestUserInfoProvider());
    }

    @AfterEach
    public void tearDown() throws Exception {
        testDataSource.getConnection()
                      .close();
    }

    @Test
    public void testAuditLogManager() {
        List<Logger> loggers = loadAuditLoggers();

        logMessage(loggers);

        assertNull(auditLogManager.getException());
    }

    private List<Logger> loadAuditLoggers() {
        return Arrays.asList(auditLogManager.getSecurityLogger(), auditLogManager.getActionLogger(), auditLogManager.getConfigLogger());
    }

    private void logMessage(List<Logger> loggers) {
        loggers.forEach(logger -> logger.info("Test Message"));
    }

    private static UserInfoProvider createTestUserInfoProvider() {
        return () -> null;
    }

}
