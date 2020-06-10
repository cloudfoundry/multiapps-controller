package org.cloudfoundry.multiapps.controller.processes.metering;

import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.CORRELATION_ID_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.END_PROCESS_EVENT_MULTIAPPS_METRIC;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.ERROR_PROCESS_EVENT_MULTIAPPS_METRIC;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.MTA_ID_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.OPERATION_STATE_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.ORG_ID_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.ORG_NAME_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.OVERALL_PROCESS_TIMER_MULTIAPPS_METRIC;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.PROCESS_MESSAGE_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.PROCESS_TYPE_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.SPACE_ID_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.SPACE_NAME_TAG;
import static org.cloudfoundry.multiapps.controller.processes.metering.MicrometerConstants.START_PROCESS_EVENT_MULTIAPPS_METRIC;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.process.mock.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.dynatrace.DynatraceMeterRegistry;

public class MicrometerNotifierImplTest {

    private final static String TEST_TAG_VALUE = "test tag value";
    private final static String LONG_ERROR_MESSAGE = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    private final static String EXPECTED_LONG_ERROR_MESSAGE = "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567";
    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    private MicrometerNotifier micrometerNotifier;

    @Mock
    private FileService fileService;
    @Mock
    private ProcessTypeParser processTypeParser;
    @Mock
    private DynatraceMeterRegistry dynatraceMeterRegistry;
    @Mock
    private Counter counter;
    @Mock
    private Timer timer;

    public MicrometerNotifierImplTest() {
        MockitoAnnotations.initMocks(this);
    }

    @BeforeEach
    public void setUp() {
        micrometerNotifier = new MicrometerNotifierImpl(dynatraceMeterRegistry, processTypeParser);
        setupStandardTags();
        Mockito.when(dynatraceMeterRegistry.counter(Mockito.anyString(), Mockito.anyIterable()))
               .thenReturn(counter);
        Mockito.when(dynatraceMeterRegistry.timer(Mockito.anyString(), Mockito.anyIterable()))
               .thenReturn(timer);
    }

    @Test
    public void testRecordStartProcessEvent() {
        micrometerNotifier.recordStartProcessEvent(execution);

        ArgumentCaptor<HashSet<Tag>> argumentCaptor = ArgumentCaptor.forClass(HashSet.class);
        Mockito.verify(dynatraceMeterRegistry)
               .counter(Mockito.eq(START_PROCESS_EVENT_MULTIAPPS_METRIC), argumentCaptor.capture());
        HashSet<Tag> actualTags = argumentCaptor.<HashSet<Tag>> getValue();

        verifyNumberOfTags(actualTags);
        verifyCommonTags(actualTags);
    }

    @Test
    public void testRecordEndProcessEvent() {
        micrometerNotifier.recordEndProcessEvent(execution);

        ArgumentCaptor<HashSet<Tag>> argumentCaptor = ArgumentCaptor.forClass(HashSet.class);
        Mockito.verify(dynatraceMeterRegistry)
               .counter(Mockito.eq(END_PROCESS_EVENT_MULTIAPPS_METRIC), argumentCaptor.capture());
        HashSet<Tag> actualTags = argumentCaptor.<HashSet<Tag>> getValue();

        verifyNumberOfTags(actualTags);
        verifyCommonTags(actualTags);
    }

    @Test
    public void testRecordErrorProcessEvent() {
        String errorMessage = "errorMessage";
        micrometerNotifier.recordErrorProcessEvent(execution, errorMessage);

        ArgumentCaptor<HashSet<Tag>> argumentCaptor = ArgumentCaptor.forClass(HashSet.class);
        Mockito.verify(dynatraceMeterRegistry)
               .counter(Mockito.eq(ERROR_PROCESS_EVENT_MULTIAPPS_METRIC), argumentCaptor.capture());
        HashSet<Tag> actualTags = argumentCaptor.<HashSet<Tag>> getValue();

        verifyNumberOfTags(actualTags);
        verifyCommonTags(actualTags);
        Assertions.assertTrue(actualTags.contains(Tag.of(PROCESS_MESSAGE_TAG, errorMessage)));
    }

    @Test
    public void testRecordErrorProcessEventWithLongErrorMessage() {
        micrometerNotifier.recordErrorProcessEvent(execution, LONG_ERROR_MESSAGE);

        ArgumentCaptor<HashSet<Tag>> argumentCaptor = ArgumentCaptor.forClass(HashSet.class);
        Mockito.verify(dynatraceMeterRegistry)
               .counter(Mockito.eq(ERROR_PROCESS_EVENT_MULTIAPPS_METRIC), argumentCaptor.capture());
        HashSet<Tag> actualTags = argumentCaptor.<HashSet<Tag>> getValue();

        verifyNumberOfTags(actualTags);
        verifyCommonTags(actualTags);
        Assertions.assertTrue(actualTags.contains(Tag.of(PROCESS_MESSAGE_TAG, EXPECTED_LONG_ERROR_MESSAGE)));
    }

    @Test
    public void testRecordOverallTime() {
        State state = State.FINISHED;
        long duration = 1;
        micrometerNotifier.recordOverallTime(execution, state, duration);

        ArgumentCaptor<HashSet<Tag>> argumentCaptor = ArgumentCaptor.forClass(HashSet.class);
        Mockito.verify(dynatraceMeterRegistry)
               .timer(Mockito.eq(OVERALL_PROCESS_TIMER_MULTIAPPS_METRIC), argumentCaptor.capture());
        Mockito.verify(timer)
               .record(duration, TimeUnit.MILLISECONDS);
        HashSet<Tag> actualTags = argumentCaptor.<HashSet<Tag>> getValue();

        verifyNumberOfTags(actualTags);
        verifyCommonTags(actualTags);
        Assertions.assertTrue(actualTags.contains(Tag.of(OPERATION_STATE_TAG, state.name())));
    }

    private void verifyCommonTags(HashSet<Tag> actualTags) {
        Assertions.assertTrue(actualTags.containsAll(buildExpectedStandardTags()));
    }

    private void verifyNumberOfTags(Set<Tag> actualTags) {
        Assertions.assertTrue(actualTags.size() <= MicrometerConstants.TAGS_COUNT_LIMIT);
    }

    private void setupStandardTags() {
        execution.setVariable(Variables.CORRELATION_ID.getName(), TEST_TAG_VALUE);
        execution.setVariable(Variables.SPACE_GUID.getName(), TEST_TAG_VALUE);
        execution.setVariable(Variables.SPACE_NAME.getName(), TEST_TAG_VALUE);
        execution.setVariable(Variables.ORGANIZATION_GUID.getName(), TEST_TAG_VALUE);
        execution.setVariable(Variables.ORGANIZATION_NAME.getName(), TEST_TAG_VALUE);
        execution.setVariable(Variables.MTA_ID.getName(), TEST_TAG_VALUE);
        Mockito.when(processTypeParser.getProcessType(execution))
               .thenReturn(ProcessType.DEPLOY);
    }

    private Set<Tag> buildExpectedStandardTags() {
        HashSet<Tag> expectedTags = new HashSet<>();
        expectedTags.add(Tag.of(CORRELATION_ID_TAG, TEST_TAG_VALUE));
        expectedTags.add(Tag.of(SPACE_ID_TAG, TEST_TAG_VALUE));
        expectedTags.add(Tag.of(SPACE_NAME_TAG, TEST_TAG_VALUE));
        expectedTags.add(Tag.of(ORG_ID_TAG, TEST_TAG_VALUE));
        expectedTags.add(Tag.of(ORG_NAME_TAG, TEST_TAG_VALUE));
        expectedTags.add(Tag.of(MTA_ID_TAG, TEST_TAG_VALUE));
        expectedTags.add(Tag.of(PROCESS_TYPE_TAG, ProcessType.DEPLOY.getName()));
        return expectedTags;
    }

}
