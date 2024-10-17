package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.UUID;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StepsTestUtil {

    public static void mockApplicationsToDeploy(List<CloudApplicationExtended> applications, DelegateExecution execution) {
        if (applications == null || applications.isEmpty()) {
            return;
        }
        var stubbing = when(execution.getVariable(Variables.APP_TO_PROCESS.getName()));
        for (var app : applications) {
            stubbing = stubbing.thenReturn(JsonUtil.toJson(app));
        }
    }

    public static void prepareDisablingAutoscaler(ProcessContext context, CloudControllerClient client, CloudApplication application,
                                                  UUID uid) {
        context.setVariable(Variables.CORRELATION_ID, "");
        when(client.getApplicationGuid(application.getName())).thenReturn(uid);
    }

    public static void testIfEnabledOrDisabledAutoscaler(CloudControllerClient client, String labelValue, UUID uid) {
        Metadata metadata = Metadata.builder()
                                    .label(MtaMetadataLabels.AUTOSCALER_LABEL, labelValue)
                                    .build();
        verify(client).updateApplicationMetadata(uid, metadata);
    }
}
