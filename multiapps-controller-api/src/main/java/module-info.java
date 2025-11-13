open module org.cloudfoundry.multiapps.controller.api {

    exports org.cloudfoundry.multiapps.controller.api;
    exports org.cloudfoundry.multiapps.controller.api.model;
    exports org.cloudfoundry.multiapps.controller.api.model.parameters;
    exports org.cloudfoundry.multiapps.controller.api.v1;
    exports org.cloudfoundry.multiapps.controller.api.v2;

    requires transitive org.cloudfoundry.multiapps.mta;
    requires transitive spring.web;

    requires org.cloudfoundry.multiapps.common;

    requires static java.compiler;
    requires static jakarta.inject;
    requires static org.immutables.value;
    requires static io.swagger.v3.oas.annotations;
    requires jakarta.servlet;
    requires jakarta.xml.bind;
    requires io.swagger.annotations;

}