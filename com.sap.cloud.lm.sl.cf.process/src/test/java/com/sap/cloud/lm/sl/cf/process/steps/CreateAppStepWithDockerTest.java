package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.cloudfoundry.client.lib.domain.DockerCredentials;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.GenericArgumentMatcher;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class CreateAppStepWithDockerTest extends CreateAppStepBaseTest {

    private DockerInfo dockerInfo;

    public void initParametersContextClient() {
        loadParameters();
        prepareContext();
        prepareConfiguration();
    }

    private void prepareContext() {
        context.setVariable(Constants.PARAM_APP_ARCHIVE_ID, "archive_id");
        context.setVariable(Constants.VAR_MODULES_INDEX, stepInput.applicationIndex);
        StepsUtil.setServicesToBind(context, Collections.emptyList());

        byte[] serviceKeysToInjectByteArray = JsonUtil.toJsonBinary(new HashMap<>());
        context.setVariable(Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT, serviceKeysToInjectByteArray);
        stepInput.applications.get(0)
            .setDockerInfo(dockerInfo);
        StepsUtil.setAppsToDeploy(context, Collections.emptyList());
        StepsTestUtil.mockApplicationsToDeploy(stepInput.applications, context);
    }

    private void loadParameters() {
        dockerInfo = createDockerInfo();
        application = stepInput.applications.get(stepInput.applicationIndex);
    }

    private DockerInfo createDockerInfo() {
        String image = "cloudfoundry/test-app";
        String username = "someUser";
        String password = "somePassword";
        DockerInfo dockerInfo = new DockerInfo(image);
        DockerCredentials dockerCredentials = new DockerCredentials(username, password);
        dockerInfo.setDockerCredentials(dockerCredentials);

        return dockerInfo;
    }

    private void prepareConfiguration() {
        Mockito.when(configuration.getPlatformType())
            .thenReturn(stepInput.platform);
    }

    @Test
    public void testWithDockerImageXS2() {
        stepInput = createStepInput(PlatformType.XS2);
        initParametersContextClient();

        step.execute(context);
        assertStepFinishedSuccessfully();

        validateClient();
    }

    @Test
    public void testWithDockerImageCF() {
        stepInput = createStepInput(PlatformType.CF);
        initParametersContextClient();

        step.execute(context);
        assertStepFinishedSuccessfully();

        validateClient();
    }

    private StepInput createStepInput(PlatformType platformType) {
        StepInput stepInput = new StepInput();

        CloudApplicationExtended cloudApplicationExtended = createFakeCloudApplicationExtended();

        stepInput.applicationIndex = 0;
        stepInput.platform = platformType;
        stepInput.applications = Arrays.asList(cloudApplicationExtended);

        return stepInput;
    }

    private CloudApplicationExtended createFakeCloudApplicationExtended() {
        CloudApplicationExtended cloudApplicationExtended = new CloudApplicationExtended(null, "application1");

        cloudApplicationExtended.setInstances(1);
        cloudApplicationExtended.setMemory(0);
        cloudApplicationExtended.setDiskQuota(512);
        cloudApplicationExtended.setEnv(Collections.emptyMap());
        cloudApplicationExtended.setServices(Collections.emptyList());
        cloudApplicationExtended.setServiceKeysToInject(Collections.emptyList());
        cloudApplicationExtended.setUris(Collections.emptyList());
        cloudApplicationExtended.setDockerInfo(dockerInfo);

        return cloudApplicationExtended;
    }

    private void validateClient() {
        Integer diskQuota = (application.getDiskQuota() != 0) ? application.getDiskQuota() : null;
        Integer memory = (application.getMemory() != 0) ? application.getMemory() : null;

        Mockito.verify(client)
            .createApplication(eq(application.getName()), argThat(GenericArgumentMatcher.forObject(application.getStaging())),
                eq(diskQuota), eq(memory), eq(application.getUris()), eq(Collections.emptyList()), eq(dockerInfo));
        Mockito.verify(client)
            .updateApplicationEnv(eq(application.getName()), eq(application.getEnvAsMap()));
    }

    @Override
    protected CreateAppStep createStep() {
        return new CreateAppStep();
    }

}
