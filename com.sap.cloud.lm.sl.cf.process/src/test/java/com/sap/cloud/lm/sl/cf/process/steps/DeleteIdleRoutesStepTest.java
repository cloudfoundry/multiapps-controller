package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationURI;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class DeleteIdleRoutesStepTest extends SyncFlowableStepTest<DeleteIdleRoutesStep> {

    private CloudApplicationExtended expectedAppToDeploy;
    private CloudApplicationExtended appToDeploy;

    public static Stream<Arguments> testExecute() {
        return Stream.of(
        // @formatter:off
            Arguments.of("app-to-deploy-1.json", Arrays.asList("module-1-idle.domain.com")),
            // (1) There are no idle URIs:
            Arguments.of("app-to-deploy-2.json", Collections.emptyList()),
            // (2) There are idle TCP URIs:
            Arguments.of("app-to-deploy-3.json", Arrays.asList("tcp://test.domain.com:51052"))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testExecute(String appDetailsFile, List<String> urisToDelete) {
        loadParameters(appDetailsFile);
        prepareContext(urisToDelete);

        step.execute(context);

        assertStepFinishedSuccessfully();

        if (CollectionUtils.isEmpty(urisToDelete)) {
            verify(client, never()).deleteRoute(anyString(), anyString());
            return;
        }

        for (String uri : urisToDelete) {
            ApplicationURI parsedUri = new ApplicationURI(uri);
            verify(client, times(1)).deleteRoute(parsedUri.getHost(), parsedUri.getDomain());
        }
    }

    private void loadParameters(String appDetailsFile) {
        expectedAppToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(appDetailsFile, getClass()),
            new TypeReference<CloudApplicationExtended>() {
            });
        appToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(appDetailsFile, getClass()),
            new TypeReference<CloudApplicationExtended>() {
            });
    }

    private void prepareContext(List<String> urisToDelete) {
        if (!urisToDelete.isEmpty()) {
            StepsUtil.setDeleteIdleUris(context, true);
        }

        CloudApplicationExtended existingApp = ImmutableCloudApplicationExtended.builder()
            .name(expectedAppToDeploy.getName())
            .uris(urisToDelete)
            .build();
        StepsUtil.setExistingApp(context, existingApp);

        StepsUtil.setApp(context, appToDeploy);
    }

    @Override
    protected DeleteIdleRoutesStep createStep() {
        return new DeleteIdleRoutesStep();
    }

}
