package com.sap.cloud.lm.sl.cf.web.bootstrap;

import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.sql.DataSource;

import org.activiti.engine.ProcessEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.auditlogging.UserInfoProvider;
import com.sap.cloud.lm.sl.cf.core.auditlogging.impl.AuditLoggingFacadeSLImpl;
import com.sap.cloud.lm.sl.cf.core.dao.DeployTargetDao;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.PersistentObject;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.changes.AsyncChange;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class BootstrapServlet extends HttpServlet {

    private static final long serialVersionUID = -1740423033397429145L;

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapServlet.class);

    @Inject
    @Qualifier("dataSource")
    protected DataSource dataSource;

    @Inject
    protected com.sap.cloud.lm.sl.cf.core.dao.v1.DeployTargetDao deployTargetDaoV1;

    @Inject
    protected com.sap.cloud.lm.sl.cf.core.dao.v2.DeployTargetDao deployTargetDaoV2;

    @Inject
    protected com.sap.cloud.lm.sl.cf.core.dao.v3.DeployTargetDao deployTargetDaoV3;

    @Inject
    protected ProcessEngine processEngine;

    @Inject
    protected ApplicationConfiguration configuration;

    @Autowired(required = false)
    private List<AsyncChange> asyncChanges;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
            configuration.load();
            initializeProviders();
            initializeActiviti();
            addDeployTargets();
            initExtras();
            executeAsyncDatabaseChanges();
            processEngine.getProcessEngineConfiguration()
                .getJobExecutor()
                .start();
            LOGGER.info(Messages.ALM_SERVICE_ENV_INITIALIZED);
        } catch (Exception e) {
            LOGGER.error("Initialization error", e);
            throw new ServletException(e);
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

    protected static UserInfoProvider getUserInfoProvider() {
        return () -> SecurityContextUtil.getUserInfo();
    }

    private void initializeProviders() throws NamingException {
        // Initialize audit logging provider
        AuditLoggingProvider.setFacade(new AuditLoggingFacadeSLImpl(dataSource, getUserInfoProvider()));
    }

    private void initializeActiviti() throws IOException {
        ActivitiFacade.getInstance()
            .init(processEngine);
    }

    private void addDeployTargets() {
        addDeployTargets(deployTargetDaoV1, new com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser(), 1);
        addDeployTargets(deployTargetDaoV2, new com.sap.cloud.lm.sl.mta.handlers.v2_0.ConfigurationParser(), 2);
        addDeployTargets(deployTargetDaoV3, new com.sap.cloud.lm.sl.mta.handlers.v3_1.ConfigurationParser(), 3);
    }

    private void addDeployTargets(DeployTargetDao dao, ConfigurationParser parser, int majorVersion) {
        List<PersistentObject<? extends Target>> existingTargets = dao.findAll();
        DescriptorHandler handler = new DescriptorHandler();
        for (Target target : configuration.getTargets(parser, majorVersion)) {
            if (!targetExists(handler, existingTargets, target)) {
                try {
                    dao.add(target);
                } catch (SLException e) {
                    LOGGER.warn(MessageFormat.format("Could not persist default target \"{0}\" to the database", target.getName()), e);
                }
            }
        }
    }

    private boolean targetExists(DescriptorHandler handler, List<PersistentObject<? extends Target>> existingTargets, Target target) {
        List<Target> rawTargets = new ArrayList<>();
        for (PersistentObject<? extends Target> t : existingTargets) {
            rawTargets.add(t.getObject());
        }
        return handler.findTarget(rawTargets, target.getName()) != null;
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
