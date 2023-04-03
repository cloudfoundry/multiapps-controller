open module org.cloudfoundry.multiapps.controller.persistence {

    exports org.cloudfoundry.multiapps.controller.persistence;
    exports org.cloudfoundry.multiapps.controller.persistence.dialects;
    exports org.cloudfoundry.multiapps.controller.persistence.dto;
    exports org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun;
    exports org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun.blobstore;
    exports org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun.blobstore.config;
    exports org.cloudfoundry.multiapps.controller.persistence.model;
    exports org.cloudfoundry.multiapps.controller.persistence.model.adapter;
    exports org.cloudfoundry.multiapps.controller.persistence.model.filters;
    exports org.cloudfoundry.multiapps.controller.persistence.query;
    exports org.cloudfoundry.multiapps.controller.persistence.query.criteria;
    exports org.cloudfoundry.multiapps.controller.persistence.query.impl;
    exports org.cloudfoundry.multiapps.controller.persistence.query.providers;
    exports org.cloudfoundry.multiapps.controller.persistence.services;
    exports org.cloudfoundry.multiapps.controller.persistence.util;

    requires transitive io.pivotal.cfenv.core;
    requires transitive io.pivotal.cfenv.jdbc;
    requires transitive java.persistence;
    requires transitive java.sql;
    requires transitive jclouds.blobstore;
    requires transitive jclouds.core;
    requires transitive org.cloudfoundry.multiapps.mta;
    requires transitive org.cloudfoundry.multiapps.controller.api;
    requires transitive org.bouncycastle.pkix;

    requires aliyun.sdk.oss;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.google.auto.service;
    requires com.google.common;
    requires com.google.guice;
    requires com.zaxxer.hikari;
    requires flowable.engine;
    requires flowable.engine.common.api;
    requires flowable.variable.service.api;
    requires java.xml.bind;
    requires javax.inject;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.apache.commons.io;
    requires org.apache.commons.collections4;
    requires org.apache.commons.lang3;
    requires org.cloudfoundry.multiapps.common;
    requires org.slf4j;
    requires spring.context;
    requires spring.core;

    requires static java.compiler;
    requires static org.immutables.value;
    requires org.postgresql.jdbc;

}
