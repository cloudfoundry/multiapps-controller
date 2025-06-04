open module org.cloudfoundry.multiapps.controller.core.test {

    exports org.cloudfoundry.multiapps.controller.core.test;

    requires transitive org.cloudfoundry.multiapps.mta;

    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.cloudfoundry.multiapps.common;
    requires org.cloudfoundry.multiapps.common.test;
    requires org.junit.jupiter.api;
    requires org.mockito;

    requires static java.compiler;
    requires static org.immutables.value;

}
