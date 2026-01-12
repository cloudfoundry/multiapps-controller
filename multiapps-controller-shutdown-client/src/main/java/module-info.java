open module org.cloudfoundry.multiapps.controller.shutdown.client {

    exports org.cloudfoundry.multiapps.controller.shutdown.client;
    exports org.cloudfoundry.multiapps.controller.shutdown.client.configuration;

    requires com.fasterxml.jackson.annotation;
    requires org.cloudfoundry.multiapps.common;
    requires org.cloudfoundry.multiapps.controller.database.migration;
    requires org.slf4j;

    requires static java.compiler;
    requires static org.immutables.value;
    requires spring.orm;

}