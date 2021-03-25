package com.sap.cloud.lm.sl.cf.web;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@ComponentScan(basePackageClasses = com.sap.cloud.lm.sl.cf.PackageMarker.class)
@ImportResource("classpath:security-context.xml")
public class SpringApplication {

}
