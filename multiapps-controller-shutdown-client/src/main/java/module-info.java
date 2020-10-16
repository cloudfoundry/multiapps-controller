open module org.cloudfoundry.multiapps.controller.shutdown.client {

    exports org.cloudfoundry.multiapps.controller.shutdown.client;
    exports org.cloudfoundry.multiapps.controller.shutdown.client.configuration;

    requires transitive org.cloudfoundry.multiapps.controller.core;

    requires com.sap.cloudfoundry.client.facade;
    requires com.fasterxml.jackson.annotation;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires org.cloudfoundry.multiapps.common;
    requires org.slf4j;

    requires static java.compiler;
    requires static org.immutables.value;

}