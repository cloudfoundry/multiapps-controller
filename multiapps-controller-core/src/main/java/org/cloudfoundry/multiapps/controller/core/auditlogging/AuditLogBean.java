package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.controller.core.auditlogging.impl.AuditLoggingFacadeSLImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.inject.Inject;
import javax.sql.DataSource;

@Configuration
public class AuditLogBean {

    @Bean
    @Inject
    public AuditLoggingFacade buildAuditLoggingFacade(DataSource dataSource, UserInfoProvider userInfoProvider) {
        return new AuditLoggingFacadeSLImpl(dataSource, userInfoProvider);
    }

    @Bean
    @Inject
    public CsrfTokenApiServiceAuditLog buildCsrfTokenApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new CsrfTokenApiServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    @Inject
    public FilesApiServiceAuditLog buildFilesApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new FilesApiServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    @Inject
    public LoginAttemptAuditLog buildLoginAttemptAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new LoginAttemptAuditLog(auditLoggingFacade);
    }

    @Bean
    @Inject
    public InfoApiServiceAuditLog buildInfoApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new InfoApiServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    @Inject
    public MtasApiServiceAuditLog buildMtasApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new MtasApiServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    @Inject
    public OperationsApiServiceAuditLog buildOperationsApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new OperationsApiServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    @Inject
    public MtaConfigurationPurgerAuditLog buildMtaConfigurationPurgerAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new MtaConfigurationPurgerAuditLog(auditLoggingFacade);
    }
}
