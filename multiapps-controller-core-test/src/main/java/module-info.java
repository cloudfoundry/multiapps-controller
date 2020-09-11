open module org.cloudfoundry.multiapps.controller.core.test {

    exports org.cloudfoundry.multiapps.controller.core.test;

    requires transitive org.apache.httpcomponents.httpcore;
    requires transitive org.cloudfoundry.multiapps.mta;

    requires org.apache.httpcomponents.httpclient;
    requires org.cloudfoundry.multiapps.common;
    requires org.cloudfoundry.multiapps.common.test;
    requires org.junit.jupiter.api;
    requires org.mockito;

    requires static java.compiler;
    requires static org.immutables.value;

}
