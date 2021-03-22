open module org.cloudfoundry.multiapps.controller.process {

    exports org.cloudfoundry.multiapps.controller.process;
    exports org.cloudfoundry.multiapps.controller.process.client;
    exports org.cloudfoundry.multiapps.controller.process.dynatrace;
    exports org.cloudfoundry.multiapps.controller.process.flowable;
    exports org.cloudfoundry.multiapps.controller.process.flowable.commands;
    exports org.cloudfoundry.multiapps.controller.process.flowable.commands.abort;
    exports org.cloudfoundry.multiapps.controller.process.jobs;
    exports org.cloudfoundry.multiapps.controller.process.listeners;
    exports org.cloudfoundry.multiapps.controller.process.metadata;
    exports org.cloudfoundry.multiapps.controller.process.metadata.parameters;
    exports org.cloudfoundry.multiapps.controller.process.steps;
    exports org.cloudfoundry.multiapps.controller.process.util;
    exports org.cloudfoundry.multiapps.controller.process.variables;

    requires transitive com.sap.cloudfoundry.client.facade;
    requires transitive flowable.engine;
    requires transitive org.cloudfoundry.multiapps.controller.api;
    requires transitive org.cloudfoundry.multiapps.controller.core;

    requires org.cloudfoundry.client;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires flowable.bpmn.model;
    requires flowable.engine.common;
    requires flowable.engine.common.api;
    requires flowable.job.service;
    requires flowable.job.service.api;
    requires flowable.variable.service;
    requires flowable.variable.service.api;
    requires java.persistence;
    requires java.sql;
    requires java.xml.bind;
    requires javax.inject;
    requires log4j;
    requires org.apache.commons.collections4;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.cloudfoundry.multiapps.common;
    requires org.cloudfoundry.multiapps.controller.client;
    requires org.cloudfoundry.multiapps.controller.persistence;
    requires org.cloudfoundry.multiapps.mta;
    requires org.eclipse.jgit;
    requires org.joda.time;
    requires org.slf4j;
    requires spring.beans;
    requires spring.context;
    requires spring.core;
    requires spring.security.oauth2.core;
    requires spring.web;
    requires reactor.netty;
    requires io.netty.handler;
    requires io.netty.transport;

    requires static java.compiler;
    requires static org.immutables.value;

}