package com.sap.cloud.lm.sl.cf.web;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan(basePackageClasses = com.sap.cloud.lm.sl.cf.PackageMarker.class)
@ImportResource("classpath:security-context.xml")
@Configuration
@EnableScheduling
public class SpringApplication {

}
