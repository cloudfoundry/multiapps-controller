package org.cloudfoundry.multiapps.controller.web.bootstrap;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.auditlogging.UserInfoProvider;
import org.cloudfoundry.multiapps.controller.core.auditlogging.impl.AuditLoggingFacadeSLImpl;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.LockOwnerService;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cmd.ClearProcessInstanceLockTimesCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

public class BootstrapServlet extends HttpServlet {

    private static final long serialVersionUID = -1740423033397429145L;

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapServlet.class);

    @Inject
    @Qualifier("dataSource")
    protected DataSource dataSource;

    @Inject
    protected ProcessEngine processEngine;

    @Inject
    protected ApplicationConfiguration configuration;

    @Inject
    @Named("fileService")
    protected FileService fileService;

    @Inject
    protected LockOwnerService lockOwnerService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
            initializeApplicationConfiguration();
            initializeProviders();
            initializeFileService();
            initExtras();
            processEngine.getProcessEngineConfiguration()
                         .getAsyncExecutor()
                         .start();
            LOGGER.info(Messages.ALM_SERVICE_ENV_INITIALIZED);
        } catch (Exception e) {
            LOGGER.error("Initialization error", e);
            throw new ServletException(e);
        }
    }

    private void initializeApplicationConfiguration() {
        configuration.load();
        LOGGER.info(format(org.cloudfoundry.multiapps.controller.core.Messages.ORG_NAME, configuration.getOrgName()));
        LOGGER.info(format(org.cloudfoundry.multiapps.controller.core.Messages.GLOBAL_CONFIG_SPACE, configuration.getGlobalConfigSpace()));
    }

    protected void initializeFileService() {
        try {
            int deletedFiles = fileService.deleteFilesEntriesWithoutContent();
            LOGGER.info(MessageFormat.format(Messages.FILE_SERVICE_DELETED_FILES, deletedFiles));
        } catch (FileStorageException e) {
            LOGGER.error(MessageFormat.format(Messages.FILE_SERVICE_CLEANUP_FAILED, e.getMessage()), e);
        }
    }

    @Override
    public void destroy() {
        clearLockOwner();
        destroyExtras();
    }

    protected void initExtras() throws NamingException {
        // Do nothing
    }

    private void clearLockOwner() {
        var lockOwner = processEngine.getProcessEngineConfiguration()
                                     .getAsyncExecutor()
                                     .getLockOwner();
        LOGGER.info(MessageFormat.format(Messages.CLEARING_LOCK_OWNER, lockOwner));
        try {
            processEngine.getProcessEngineConfiguration()
                         .getCommandExecutor()
                         .execute(new ClearProcessInstanceLockTimesCmd(lockOwner));
            lockOwnerService.createQuery()
                            .lockOwner(lockOwner)
                            .delete();
            LOGGER.info(MessageFormat.format(Messages.CLEARED_LOCK_OWNER, lockOwner));
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format(Messages.CLEARING_FLOWABLE_LOCK_OWNER_THREW_AN_EXCEPTION_0, e.getMessage()), e);
        }
    }

    protected void destroyExtras() {
        // Do nothing
    }

    protected static UserInfoProvider getUserInfoProvider() {
        return SecurityContextUtil::getUserInfo;
    }

    private void initializeProviders() {
        // Initialize audit logging provider
        AuditLoggingProvider.setFacade(new AuditLoggingFacadeSLImpl(dataSource, getUserInfoProvider()));
    }

}
