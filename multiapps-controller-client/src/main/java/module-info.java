open module org.cloudfoundry.multiapps.controller.client {

    exports org.cloudfoundry.multiapps.controller.client;
    exports org.cloudfoundry.multiapps.controller.client.lib.domain;
    exports org.cloudfoundry.multiapps.controller.client.uaa;
    exports org.cloudfoundry.multiapps.controller.client.util;
    exports org.cloudfoundry.multiapps.controller.client.facade;
    exports org.cloudfoundry.multiapps.controller.client.facade.rest;
    exports org.cloudfoundry.multiapps.controller.client.facade.oauth2;
    exports org.cloudfoundry.multiapps.controller.client.facade.domain;
    exports org.cloudfoundry.multiapps.controller.client.facade.adapters;
    exports org.cloudfoundry.multiapps.controller.client.facade.util;
    exports org.cloudfoundry.multiapps.controller.client.facade.dto;

    requires spring.security.oauth2.core;
    requires transitive spring.web;

    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    requires java.net.http;
    requires io.netty.handler;
    requires io.netty.transport;
    requires org.apache.commons.collections4;
    requires org.apache.commons.io;
    requires org.apache.commons.logging;
    requires org.cloudfoundry.multiapps.common;
    requires org.reactivestreams;
    requires org.slf4j;
    requires reactor.core;
    requires reactor.netty;
    requires reactor.netty.core;
    requires reactor.netty.http;
    requires spring.core;
    requires spring.security.core;
    requires spring.security.oauth2.client;
    requires spring.webflux;

    requires static com.fasterxml.jackson.annotation;
    requires static java.compiler;
    requires static jakarta.inject;
    requires static org.immutables.value;
    requires io.netty.codec;
    requires org.apache.logging.log4j;

}