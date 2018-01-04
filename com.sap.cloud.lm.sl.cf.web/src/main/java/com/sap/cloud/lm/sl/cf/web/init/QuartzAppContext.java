package com.sap.cloud.lm.sl.cf.web.init;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

import com.sap.cloud.lm.sl.cf.process.jobs.CleanUpJob;

@Configuration
public class QuartzAppContext {

    public final static String CLEAN_UP_TRIGGER_NAME = "cleanUpTrigger";
    public final static String TRIGGER_GROUP = "DEFAULT";

    @Bean(name = "cleanUpJobDetail")
    public JobDetailFactoryBean jobDetailFactoryBean() {
        JobDetailFactoryBean factory = new JobDetailFactoryBean();
        factory.setJobClass(CleanUpJob.class);
        factory.setDurability(true);
        return factory;
    }

    @Bean(name = "cleanUpCronTriggerFactoryBean")
    public CronTriggerFactoryBean cronTriggerFactoryBean() {
        com.sap.cloud.lm.sl.cf.core.util.Configuration configuration = com.sap.cloud.lm.sl.cf.core.util.Configuration.getInstance();
        CronTriggerFactoryBean factory = new CronTriggerFactoryBean();
        factory.setJobDetail(jobDetailFactoryBean().getObject());
        factory.setCronExpression(configuration.getCronExpressionForOldData());
        factory.setGroup(TRIGGER_GROUP);
        factory.setName(CLEAN_UP_TRIGGER_NAME);
        return factory;
    }
}
