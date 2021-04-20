open module org.cloudfoundry.multiapps.controller.database.migration {

    exports org.cloudfoundry.multiapps.controller.database.migration;
    exports org.cloudfoundry.multiapps.controller.database.migration.client;
    exports org.cloudfoundry.multiapps.controller.database.migration.executor;
    exports org.cloudfoundry.multiapps.controller.database.migration.executor.type;
    exports org.cloudfoundry.multiapps.controller.database.migration.extractor;
    exports org.cloudfoundry.multiapps.controller.database.migration.generator;
    exports org.cloudfoundry.multiapps.controller.database.migration.metadata;

    requires transitive org.cloudfoundry.multiapps.controller.persistence;

    requires java.naming;
    requires java.sql;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.cloudfoundry.multiapps.common;
    requires org.slf4j;

    requires static java.compiler;
    requires static org.immutables.value;

}