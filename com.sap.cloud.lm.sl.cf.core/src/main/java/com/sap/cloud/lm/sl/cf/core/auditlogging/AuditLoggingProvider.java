package com.sap.cloud.lm.sl.cf.core.auditlogging;

public class AuditLoggingProvider {

    private static AuditLoggingFacade facade;

    private AuditLoggingProvider() {
    }

    public static void setFacade(AuditLoggingFacade facade) {
        AuditLoggingProvider.facade = facade;
    }

    public static AuditLoggingFacade getFacade() {
        return facade;
    }

}
