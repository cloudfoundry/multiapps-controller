package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableDockerCredentials;
import org.cloudfoundry.client.lib.domain.ImmutableDockerInfo;
import org.cloudfoundry.multiapps.common.util.GenericArgumentMatcher;
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class CreateOrUpdateAppStepWithDockerTest extends CreateOrUpdateAppStepBaseTest {

    private static final DockerInfo DOCKER_INFO = createDockerInfo();

    private static DockerInfo createDockerInfo() {
        String image = "cloudfoundry/test-app";
        String username = "someUser";
        String password = "somePassword";
        return ImmutableDockerInfo.builder()
                                  .image(image)
                                  .credentials(ImmutableDockerCredentials.builder()
                                                                         .username(username)
                                                                         .password(password)
                                                                         .build())
                                  .build();
    }

    @Test
    public void testWithDockerImage() {
        stepInput = createStepInput();
        loadParameters();
        prepareContext();
        prepareClient();

        step.execute(execution);
        assertStepFinishedSuccessfully();

        validateClient();
    }

    private void prepareClient() {
        Mockito.doReturn(ImmutableCloudApplication.builder()
                                                  .metadata(ImmutableCloudMetadata.builder()
                                                                                  .guid(UUID.randomUUID())
                                                                                  .build())
                                                  .build())
               .when(client)
               .getApplication(application.getName());
    }

    private void prepareContext() {
        context.setVariable(Variables.APP_ARCHIVE_ID, "archive_id");
        context.setVariable(Variables.MODULES_INDEX, stepInput.applicationIndex);
        context.setVariable(Variables.SERVICES_TO_BIND, Collections.emptyList());

        context.setVariable(Variables.SERVICE_KEYS_CREDENTIALS_TO_INJECT, new HashMap<>());
        context.setVariable(Variables.APPS_TO_DEPLOY, Collections.emptyList());
        StepsTestUtil.mockApplicationsToDeploy(stepInput.applications, execution);
    }

    private void loadParameters() {
        application = stepInput.applications.get(stepInput.applicationIndex);
    }

    private StepInput createStepInput() {
        StepInput stepInput = new StepInput();

        CloudApplicationExtended cloudApplicationExtended = createFakeCloudApplicationExtended();

        stepInput.applicationIndex = 0;
        stepInput.applications = Collections.singletonList(cloudApplicationExtended);

        return stepInput;
    }

    private CloudApplicationExtended createFakeCloudApplicationExtended() {
        return ImmutableCloudApplicationExtended.builder()
                                                .name("application1")
                                                .instances(1)
                                                .memory(0)
                                                .diskQuota(512)
                                                .dockerInfo(DOCKER_INFO)
                                                .build();
    }

    private void validateClient() {
        Integer diskQuota = (application.getDiskQuota() != 0) ? application.getDiskQuota() : null;
        Integer memory = (application.getMemory() != 0) ? application.getMemory() : null;

        Mockito.verify(client)
               .createApplication(eq(application.getName()), argThat(GenericArgumentMatcher.forObject(application.getStaging())),
                                  eq(diskQuota), eq(memory), eq(application.getUris()), eq(DOCKER_INFO));
        Mockito.verify(client)
               .updateApplicationEnv(eq(application.getName()), eq(application.getEnv()));
    }

    @Override
    protected CreateOrUpdateAppStep createStep() {
        return new CreateOrUpdateAppStep();
    }

}
