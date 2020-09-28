open module org.cloudfoundry.multiapps.controller.persistence {

    exports org.cloudfoundry.multiapps.controller.persistence;
    exports org.cloudfoundry.multiapps.controller.persistence.dialects;
    exports org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun;
    exports org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun.blobstore;
    exports org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun.blobstore.config;
    exports org.cloudfoundry.multiapps.controller.persistence.model;
    exports org.cloudfoundry.multiapps.controller.persistence.query;
    exports org.cloudfoundry.multiapps.controller.persistence.query.providers;
    exports org.cloudfoundry.multiapps.controller.persistence.services;
    exports org.cloudfoundry.multiapps.controller.persistence.util;

    requires transitive java.cfenv;
    requires transitive java.cfenv.jdbc;
    requires transitive java.sql;
    requires transitive jclouds.blobstore;
    requires transitive jclouds.core;

    requires HikariCP.java7;
    requires aliyun.sdk.oss;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.google.auto.service;
    requires com.google.common;
    requires error.prone.annotations;
    requires flowable.engine;
    requires flowable.engine.common.api;
    requires flowable.variable.service.api;
    requires guice;
    requires java.xml.bind;
    requires javax.inject;
    requires log4j;
    requires org.apache.commons.io;
    requires org.cloudfoundry.multiapps.common;
    requires org.slf4j;
    requires spring.context;
    requires spring.core;

    requires static java.compiler;
    requires static org.immutables.value;

}
