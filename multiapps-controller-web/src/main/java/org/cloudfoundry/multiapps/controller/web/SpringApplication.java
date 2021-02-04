package org.cloudfoundry.multiapps.controller.web;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@ComponentScan(basePackageClasses = org.cloudfoundry.multiapps.controller.PackageMarker.class)
//@ImportResource("classpath:security-context.xml")
public class SpringApplication {

}
