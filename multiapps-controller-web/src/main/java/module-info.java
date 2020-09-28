open module org.cloudfoundry.multiapps.controller.web {

    exports org.cloudfoundry.multiapps.controller.web;
    exports org.cloudfoundry.multiapps.controller.web.api.impl;
    exports org.cloudfoundry.multiapps.controller.web.bootstrap;
    exports org.cloudfoundry.multiapps.controller.web.configuration;
    exports org.cloudfoundry.multiapps.controller.web.configuration.bean;
    exports org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;
    exports org.cloudfoundry.multiapps.controller.web.configuration.service;
    exports org.cloudfoundry.multiapps.controller.web.interceptors;
    exports org.cloudfoundry.multiapps.controller.web.listeners;
    exports org.cloudfoundry.multiapps.controller.web.monitoring;
    exports org.cloudfoundry.multiapps.controller.web.resources;
    exports org.cloudfoundry.multiapps.controller.web.security;
    exports org.cloudfoundry.multiapps.controller.web.util;

    requires transitive flowable.job.service;
    requires transitive flowable.spring;
    requires transitive io.github.resilience4j.ratelimiter;
    requires transitive java.sql;
    requires transitive liquibase.core;
    requires transitive micrometer.registry.jmx;
    requires transitive org.cloudfoundry.multiapps.controller.api;
    requires transitive org.cloudfoundry.multiapps.controller.core;
    requires transitive org.cloudfoundry.multiapps.controller.process;
    requires transitive quartz;
    requires transitive spring.context;
    requires transitive spring.context.support;
    requires transitive spring.jdbc;
    requires transitive spring.orm;
    requires transitive spring.security.core;
    requires transitive spring.security.oauth2;
    requires transitive spring.security.web;
    requires transitive spring.tx;
    requires transitive spring.webmvc;

    requires cloudfoundry.client.lib;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.google.common;
    requires commons.fileupload;
    requires flowable.engine;
    requires flowable.engine.common;
    requires flowable.engine.common.api;
    requires flowable.spring.common;
    requires java.naming;
    requires java.persistence;
    requires java.servlet;
    requires java.xml;
    requires java.xml.bind;
    requires javax.inject;
    requires jclouds.blobstore;
    requires jclouds.core;
    requires micrometer.core;
    requires org.apache.commons.collections4;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.cloudfoundry.multiapps.common;
    requires org.cloudfoundry.multiapps.controller.client;
    requires org.cloudfoundry.multiapps.controller.persistence;
    requires org.cloudfoundry.multiapps.mta;
    requires org.mybatis;
    requires org.slf4j;
    requires spring.beans;
    requires spring.core;
    requires spring.web;

    requires static java.compiler;
    requires static org.immutables.value;

}