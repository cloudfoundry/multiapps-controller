open module org.cloudfoundry.multiapps.controller.shutdown.client {

    exports org.cloudfoundry.multiapps.controller.shutdown.client;
    exports org.cloudfoundry.multiapps.controller.shutdown.client.configuration;

    requires transitive org.cloudfoundry.multiapps.controller.core;

    requires com.fasterxml.jackson.annotation;
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.cloudfoundry.multiapps.controller.client;
    requires org.cloudfoundry.multiapps.common;
    requires org.slf4j;

    requires static java.compiler;
    requires static org.immutables.value;

}