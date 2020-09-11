open module org.cloudfoundry.multiapps.controller.persistence.test {

    exports org.cloudfoundry.multiapps.controller.persistence.test;

    requires transitive java.sql;

    requires liquibase.core;
    requires org.cloudfoundry.multiapps.common;
    requires spring.beans;
    requires spring.jdbc;

}
