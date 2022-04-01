package com.sap.cloud.lm.sl.cf.web.bootstrap;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;

import org.flowable.engine.ProcessEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.auditlogging.UserInfoProvider;
import com.sap.cloud.lm.sl.cf.core.auditlogging.impl.AuditLoggingFacadeSLImpl;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.changes.AsyncChange;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;

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

    @Autowired(required = false)
    private List<AsyncChange> asyncChanges;

    protected static UserInfoProvider getUserInfoProvider() {
        return SecurityContextUtil::getUserInfo;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
            configuration.load();
            initializeProviders();
            initializeFileService();
            initExtras();
            executeAsyncDatabaseChanges();
            processEngine.getProcessEngineConfiguration()
                         .getAsyncExecutor()
                         .start();
            LOGGER.info(Messages.ALM_SERVICE_ENV_INITIALIZED);
        } catch (Exception e) {
            LOGGER.error("Initialization error", e);
            throw new ServletException(e);
        }
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
        destroyExtras();
    }

    protected void initExtras() throws Exception {
        // Do nothing
    }

    protected void destroyExtras() {
        // Do nothing
    }

    private void initializeProviders() {
        // Initialize audit logging provider
        AuditLoggingProvider.setFacade(new AuditLoggingFacadeSLImpl(dataSource, getUserInfoProvider()));
    }

    private void executeAsyncDatabaseChanges() {
        if (asyncChanges == null || asyncChanges.isEmpty()) {
            return;
        }
        Integer appInstanceIndex = configuration.getApplicationInstanceIndex();
        // Problems may arise if the changes are executed in parallel on multiple instances. Since there will always be *at least* one
        // instance, we always execute the changes on the first.
        if (appInstanceIndex == null || appInstanceIndex != 0) {
            LOGGER.info(MessageFormat.format(Messages.ASYNC_DATABASE_CHANGES_WILL_NOT_BE_EXECUTED_ON_THIS_INSTANCE, appInstanceIndex));
            return;
        }
        for (AsyncChange asyncChange : asyncChanges) {
            new Thread(toRunnable(asyncChange)).start();
        }
    }

    private Runnable toRunnable(AsyncChange asyncChange) {
        return () -> {
            try {
                asyncChange.execute(dataSource);
            } catch (SQLException e) {
                LOGGER.error(e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            }
        };
    }

}
