package org.cloudfoundry.multiapps.controller.processes.metering;

public class MicrometerConstants {
    
    // Meter names
    public static final String MULTIAPPS_METRICS_PREFIX = "multiapps.";
    private static final String PROCESS_EVENT_MULTIAPPS_METRICS_PREFIX= MULTIAPPS_METRICS_PREFIX + "processes.event.";
    public static final String START_PROCESS_EVENT_MULTIAPPS_METRIC = PROCESS_EVENT_MULTIAPPS_METRICS_PREFIX + "start";
    public static final String END_PROCESS_EVENT_MULTIAPPS_METRIC = PROCESS_EVENT_MULTIAPPS_METRICS_PREFIX + "end";
    public static final String ERROR_PROCESS_EVENT_MULTIAPPS_METRIC = PROCESS_EVENT_MULTIAPPS_METRICS_PREFIX + "error";
    private static final String PROCESS_TIMER_MULTIAPPS_METRICS_PREFIX= MULTIAPPS_METRICS_PREFIX + "processes.timer.";
    public static final String OVERALL_PROCESS_TIMER_MULTIAPPS_METRIC = PROCESS_TIMER_MULTIAPPS_METRICS_PREFIX + "overall";
    
    // Meter tag
    public static final String CORRELATION_ID_TAG = "correlation.id";
    public static final String SPACE_ID_TAG = "space.id";
    public static final String SPACE_NAME_TAG = "space.name";
    public static final String ORG_ID_TAG = "org.id";
    public static final String ORG_NAME_TAG = "org.name";
    public static final String MTA_ID_TAG = "mta.id";
    public static final String PROCESS_TYPE_TAG = "process.type";
    public static final String OPERATION_STATE_TAG = "operation.state";
    public static final String PROCESS_MESSAGE_TAG = "process.message";
    public static final String DEFAULT_TAG_VALUE = "N/A";
    
    // Micrometer limitations
    public static final int TAGS_COUNT_LIMIT = 10;
    public static final int DYNATRACE_DIMENSION_MAX_VALUE_LENGTH = 128;
    
}
