package org.cloudfoundry.multiapps.controller.process.flowable;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;

@Named
public class ProcessActionRegistry {

    private final List<ProcessAction> processActions;

    @Inject
    public ProcessActionRegistry(List<ProcessAction> processActions) {
        this.processActions = processActions;
    }

    public ProcessAction getAction(Action action) {
        return processActions.stream()
                             .filter(processAction -> processAction.getAction() == action)
                             .findFirst()
                             .orElseThrow(() -> new IllegalStateException(MessageFormat.format(Messages.NO_PROCESS_ACTION_FOUND,
                                                                                               action.getActionId())));
    }

}
