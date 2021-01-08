package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;

@Named
public class StartProcessAction extends ResumeProcessAction {

    @Inject
    public StartProcessAction(FlowableFacade flowableFacade, List<AdditionalProcessAction> additionalProcessActions,
                              OperationService operationService, CloudControllerClientProvider cloudControllerClientProvider) {
        super(flowableFacade, additionalProcessActions, operationService, cloudControllerClientProvider);
    }

    @Override
    public Action getAction() {
        return Action.START;
    }
}
