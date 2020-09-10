package org.cloudfoundry.multiapps.controller.process.flowable;

import java.text.MessageFormat;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.process.Messages;

public enum Action {

    ABORT, RESUME, RETRY, START;

    private static final Map<String, Action> namesToActions = Stream.of(values())
                                                                    .collect(Collectors.toMap(Action::getActionId, Function.identity()));

    public String getActionId() {
        return name().toLowerCase();
    }

    public static Action fromString(String actionId) {
        Action action = namesToActions.get(actionId);
        if (action == null) {
            throw new IllegalArgumentException(MessageFormat.format(Messages.UNSUPPORTED_ACTION, actionId));
        }
        return action;
    }
}
