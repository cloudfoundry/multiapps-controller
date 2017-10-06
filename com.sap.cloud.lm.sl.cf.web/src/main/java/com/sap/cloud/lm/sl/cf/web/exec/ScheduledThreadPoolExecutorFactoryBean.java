package com.sap.cloud.lm.sl.cf.web.exec;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;

public class ScheduledThreadPoolExecutorFactoryBean implements FactoryBean<ScheduledExecutorService>, InitializingBean, DisposableBean {

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    private int coreThreads;

    @Override
    public ScheduledExecutorService getObject() {
        return scheduledThreadPoolExecutor;
    }

    @Override
    public Class<?> getObjectType() {
        return ScheduledExecutorService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() {
        coreThreads = ConfigurationUtil.getAsyncExecutorCoreThreads();

        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(coreThreads);
    }

    @Override
    public void destroy() {
        scheduledThreadPoolExecutor.shutdown();
    }

}
