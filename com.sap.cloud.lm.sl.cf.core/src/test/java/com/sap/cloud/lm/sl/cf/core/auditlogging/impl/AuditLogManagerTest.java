package com.sap.cloud.lm.sl.cf.core.auditlogging.impl;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.auditlogging.UserInfoProvider;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.common.util.TestDataSourceProvider;

public class AuditLogManagerTest {

    private static final String AUDIT_LOG_CHANGELOG_LOCATION = "com/sap/cloud/lm/sl/cf/core/db/changelog/db-changelog.xml";
    private DataSource testDataSource;
    private AuditLogManager auditLogManager;

    private static UserInfoProvider createTestUserInfoProvider() {
        return new UserInfoProvider() {
            @Override
            public UserInfo getUserInfo() {
                return null;
            }
        };
    }

    @Before
    public void setUp() throws Exception {
        testDataSource = TestDataSourceProvider.getDataSource(AUDIT_LOG_CHANGELOG_LOCATION);
        auditLogManager = new AuditLogManager(testDataSource, createTestUserInfoProvider());
    }

    @After
    public void tearDown() throws Exception {
        testDataSource.getConnection()
                      .close();
    }

    @Test
    public void test() throws Exception {
        auditLogManager.getSecurityLogger()
                       .info("That's a security message");
        Exception e = auditLogManager.getException();
        if (e != null) {
            throw e;
        }
    }

}
