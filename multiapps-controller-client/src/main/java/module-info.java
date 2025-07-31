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

    requires transitive org.cloudfoundry.client;
    requires spring.security.oauth2.core;
    requires transitive spring.web;

    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.collections4;
    requires org.apache.commons.io;
    requires org.apache.commons.logging;
    requires org.cloudfoundry.client.reactor;
    requires org.cloudfoundry.multiapps.common;
    requires org.cloudfoundry.util;
    requires org.slf4j;
    requires java.net.http;
    requires spring.core;
    requires spring.webflux;
    requires reactor.core;
    requires reactor.netty.core;
    requires reactor.netty.http;
    requires org.reactivestreams;
    requires io.netty.handler;
    requires io.netty.transport;
    requires reactor.netty;
    requires spring.security.core;
    requires spring.security.oauth2.client;

    requires static com.fasterxml.jackson.annotation;
    requires static java.compiler;
    requires static jakarta.inject;
    requires static org.immutables.value;

}