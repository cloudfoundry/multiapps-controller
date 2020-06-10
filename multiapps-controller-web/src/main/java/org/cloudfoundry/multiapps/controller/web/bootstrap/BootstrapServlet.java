package org.cloudfoundry.multiapps.controller.web.bootstrap;

import static java.text.MessageFormat.format;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Timer;

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
import org.cloudfoundry.multiapps.controller.persistence.changes.AsyncChange;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.timers.RegularTimer;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.flowable.engine.ProcessEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired(required = false)
    private List<AsyncChange> asyncChanges;
    
    @Autowired(required = false)
    private List<RegularTimer> regularTimers;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
            initializeApplicationConfiguration();
            initializeProviders();
            initializeFileService();
            initExtras();
            executeAsyncDatabaseChanges();
            initTimers();
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
        destroyExtras();
    }

    protected void initExtras() throws NamingException {
        // Do nothing
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
    
    private void initTimers() {
        LOGGER.warn("regulartimers: " + regularTimers);
        if (regularTimers == null || regularTimers.isEmpty()) {
            return;
        }
        Timer timer = new Timer(true);
        for (RegularTimer regularTimer : regularTimers) {
            LOGGER.warn("initTimers schedule taskL " + regularTimer);
            timer.scheduleAtFixedRate(regularTimer.getTimerTask(), regularTimer.getDelay(), regularTimer.getPeriod());
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
