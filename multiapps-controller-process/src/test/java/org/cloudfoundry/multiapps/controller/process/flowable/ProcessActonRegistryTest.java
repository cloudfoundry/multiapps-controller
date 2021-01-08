package org.cloudfoundry.multiapps.controller.process.flowable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ProcessActonRegistryTest {

    private ProcessActionRegistry processActionRegistry;

    @BeforeEach
    void setUp() {
        processActionRegistry = new ProcessActionRegistry(getProcessActions());
    }

    static Stream<Arguments> testAction() {
        //@formatter:off
        return Stream.of(Arguments.of(Action.ABORT),
                         Arguments.of(Action.RESUME),
                         Arguments.of(Action.RETRY),
                         Arguments.of(Action.START));
        //@formatter:on
    }

    @ParameterizedTest
    @MethodSource
    void testAction(Action action) {
        ProcessAction abortProcessAction = processActionRegistry.getAction(action);
        assertEquals(action, abortProcessAction.getAction());
    }

    @Test
    void testGetInvalidOperation() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> Action.fromString("invalid"));
        assertEquals(MessageFormat.format(Messages.UNSUPPORTED_ACTION, "invalid"), exception.getMessage());
    }

    private List<ProcessAction> getProcessActions() {
        return List.of(new StartProcessAction(null, null, null, null), new ResumeProcessAction(null, null, null, null),
                       new RetryProcessAction(null, null, null, null, null), new AbortProcessAction(null, null, null, null, null, null, null));
    }

}