open module org.cloudfoundry.multiapps.controller.api {

    exports org.cloudfoundry.multiapps.controller.api;
    exports org.cloudfoundry.multiapps.controller.api.model;
    exports org.cloudfoundry.multiapps.controller.api.model.parameters;
    exports org.cloudfoundry.multiapps.controller.api.v1;
    exports org.cloudfoundry.multiapps.controller.api.v2;

    requires transitive java.servlet;
    requires transitive org.cloudfoundry.multiapps.mta;
    requires transitive spring.web;

    requires org.cloudfoundry.multiapps.common;

    requires static java.compiler;
    requires static java.xml.bind;
    requires static javax.inject;
    requires static org.immutables.value;
    requires static swagger.annotations;

}