package com.sap.cloud.lm.sl.cf.web.configuration;

import java.util.TimeZone;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;

@Configuration
public class SchedulerConfiguration {

    @Inject
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                                                                                  .withJdbcTemplate(new JdbcTemplate(dataSource))
                                                                                  .withTimeZone(TimeZone.getDefault())
                                                                                  .build());
    }
}
