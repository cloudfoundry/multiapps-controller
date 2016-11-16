package com.sap.cloud.lm.sl.cf.web.bootstrap;

import java.io.IOException;
import java.text.MessageFormat;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.auditlogging.UserInfoProvider;
import com.sap.cloud.lm.sl.cf.core.auditlogging.impl.AuditLoggingFacadeSLImpl;
import com.sap.cloud.lm.sl.cf.core.dao.TargetPlatformDao;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.metadata.CtsDeployServiceMetadata;
import com.sap.cloud.lm.sl.cf.process.metadata.CtsPingServiceMetadata;
import com.sap.cloud.lm.sl.cf.process.metadata.XS2BlueGreenDeployServiceMetadata;
import com.sap.cloud.lm.sl.cf.process.metadata.XS2DeployServiceMetadata;
import com.sap.cloud.lm.sl.cf.process.metadata.XS2UndeployServiceMetadata;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.persistence.dialects.DatabaseDialect;
import com.sap.cloud.lm.sl.slp.ServiceRegistry;
import com.sap.cloud.lm.sl.slp.activiti.ActivitiFacade;

public class BootstrapServlet extends HttpServlet {

    private static final long serialVersionUID = -1740423033397429145L;

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapServlet.class);

    @Inject
    @Qualifier("dataSource")
    protected DataSource dataSource;

    @Inject
    protected DatabaseDialect databaseDialect;

    @Inject
    protected com.sap.cloud.lm.sl.cf.core.dao.v1.TargetPlatformDao targetPlatformDaoV1;

    @Inject
    protected com.sap.cloud.lm.sl.cf.core.dao.v2.TargetPlatformDao targetPlatformDaoV2;

    @Inject
    protected com.sap.cloud.lm.sl.cf.core.dao.v3.TargetPlatformDao targetPlatformDaoV3;

    @Inject
    protected ProcessEngine processEngine;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
            ConfigurationUtil.load();
            initializeProviders();
            initializeActiviti();
            initializeServices();
            addTargetPlatforms();
            initExtras();
            ConfigurationUtil.logFullConfig();
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
        ActivitiFacade.getInstance().init(processEngine);
    }

    private void initializeServices() {
        ServiceRegistry.getInstance().addService(new XS2DeployServiceMetadata());
        ServiceRegistry.getInstance().addService(new CtsDeployServiceMetadata());
        ServiceRegistry.getInstance().addService(new CtsPingServiceMetadata());
        ServiceRegistry.getInstance().addService(new XS2BlueGreenDeployServiceMetadata());
        ServiceRegistry.getInstance().addService(new XS2UndeployServiceMetadata());
    }

    private void addTargetPlatforms() {
        addTargetPlatforms(targetPlatformDaoV1, new com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser(), 1);
        addTargetPlatforms(targetPlatformDaoV2, new com.sap.cloud.lm.sl.mta.handlers.v2_0.ConfigurationParser(), 2);
        addTargetPlatforms(targetPlatformDaoV3, new com.sap.cloud.lm.sl.mta.handlers.v3_1.ConfigurationParser(), 3);
    }

    private void addTargetPlatforms(TargetPlatformDao dao, ConfigurationParser parser, int majorVersion) {
        List<TargetPlatform> existingPlatforms = dao.findAll();
        DescriptorHandler handler = new DescriptorHandler();
        for (TargetPlatform platform : ConfigurationUtil.getPlatforms(parser, majorVersion)) {
            if (!platformExists(handler, existingPlatforms, platform)) {
                try {
                    dao.add(platform);
                } catch (SLException e) {
                    LOGGER.warn(MessageFormat.format("Could not persist default platform \"{0}\" to the database", platform.getName()), e);
                }
            }
        }
    }

    private static boolean platformExists(DescriptorHandler handler, List<TargetPlatform> existingPlatforms, TargetPlatform platform) {
        return handler.findPlatform(existingPlatforms, platform.getName()) != null;
    }

}
