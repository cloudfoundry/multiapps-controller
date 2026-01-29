package org.cloudfoundry.multiapps.controller.shutdown.client;

public class Messages {

    private Messages() {
    }

    public static final String SHUTDOWN_STATUS_OF_APPLICATION_WITH_GUID_INSTANCE = "Shutdown status of application with GUID {0}, instance {1}: {2}";
    public static final String APP_INSTANCE_WITH_ID_AND_INDEX_SCHEDULED_FOR_SHUTDOWN = "Application with ID \"{0}\" and index \"{1}\" has been scheduled for shutdown";
    public static final String FINISHED_SHUTTING_DOWN = "Finished shutting down";
}
