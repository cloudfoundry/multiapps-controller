open module org.cloudfoundry.multiapps.controller.client {

    exports org.cloudfoundry.multiapps.controller.client;
    exports org.cloudfoundry.multiapps.controller.client.lib.domain;
    exports org.cloudfoundry.multiapps.controller.client.uaa;
    exports org.cloudfoundry.multiapps.controller.client.util;

    requires transitive org.cloudfoundry.client;
    requires transitive com.sap.cloudfoundry.client.facade;
    requires spring.security.oauth2.core;
    requires transitive spring.web;

    requires org.apache.commons.collections4;
    requires org.cloudfoundry.multiapps.common;
    requires org.slf4j;
    requires spring.core;
    requires spring.webflux;
    requires reactor.core;
    requires org.reactivestreams;
    requires io.netty.handler;
    requires reactor.netty;
    requires spring.security.core;
    requires spring.security.oauth2.client;

    requires static com.fasterxml.jackson.annotation;
    requires static java.compiler;
    requires static javax.inject;
    requires static org.immutables.value;

}