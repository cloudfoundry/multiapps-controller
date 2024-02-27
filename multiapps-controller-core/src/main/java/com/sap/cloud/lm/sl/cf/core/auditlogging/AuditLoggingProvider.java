package com.sap.cloud.lm.sl.cf.core.auditlogging;

public class AuditLoggingProvider {

    private static AuditLoggingFacade facade;

    public static AuditLoggingFacade getFacade() {
        return facade;
    }

    public static void setFacade(AuditLoggingFacade facade) {
        AuditLoggingProvider.facade = facade;
    }

}
