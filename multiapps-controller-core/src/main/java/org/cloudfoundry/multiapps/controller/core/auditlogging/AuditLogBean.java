package org.cloudfoundry.multiapps.controller.core.auditlogging;

import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.core.auditlogging.impl.AuditLoggingFacadeSLImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditLogBean {

    @Bean
    public AuditLoggingFacade buildAuditLoggingFacade(DataSource dataSource, UserInfoProvider userInfoProvider) {
        return new AuditLoggingFacadeSLImpl(dataSource, userInfoProvider);
    }

    @Bean
    public CsrfTokenApiServiceAuditLog buildCsrfTokenApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new CsrfTokenApiServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    public FilesApiServiceAuditLog buildFilesApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new FilesApiServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    public LoginAttemptAuditLog buildLoginAttemptAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new LoginAttemptAuditLog(auditLoggingFacade);
    }

    @Bean
    public InfoApiServiceAuditLog buildInfoApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new InfoApiServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    public MtasApiServiceAuditLog buildMtasApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new MtasApiServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    public OperationsApiServiceAuditLog buildOperationsApiServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new OperationsApiServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    public MtaConfigurationPurgerAuditLog buildMtaConfigurationPurgerAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new MtaConfigurationPurgerAuditLog(auditLoggingFacade);
    }

    @Bean
    public ConfigurationSubscriptionServiceAuditLog buildAConfigurationSubscriptionServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new ConfigurationSubscriptionServiceAuditLog(auditLoggingFacade);
    }

    @Bean
    public ConfigurationEntryServiceAuditLog buildAConfigurationEntryServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        return new ConfigurationEntryServiceAuditLog(auditLoggingFacade);
    }
}
