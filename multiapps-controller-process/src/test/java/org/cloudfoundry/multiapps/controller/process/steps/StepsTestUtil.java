package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.mockito.Mockito;

public class StepsTestUtil {

    public static void mockApplicationsToDeploy(List<CloudApplicationExtended> applications, DelegateExecution execution) {
        if (applications == null || applications.isEmpty()) {
            return;
        }
        var stubbing = Mockito.when(execution.getVariable(Variables.APP_TO_PROCESS.getName()));
        for (var app : applications) {
            stubbing = stubbing.thenReturn(JsonUtil.toJson(app));
        }
    }
}
