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
import com.sap.cloud.lm.sl.cf.core.util.ApplicationURI;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class DeleteIdleRoutesStepTest extends SyncFlowableStepTest<DeleteIdleRoutesStep> {

    public static Stream<Arguments> testExecute() {
        return Stream.of(
        // @formatter:off
            // (1) One old URI is replaced with a new one in redeploy:
            Arguments.of("existing-app-1.json", "app-to-deploy-1.json", Arrays.asList("module-1.domain.com")),
            // (2) There are no differences between old and new URIs:
            Arguments.of("existing-app-2.json", "app-to-deploy-2.json", Collections.emptyList()),
            // (3) The new URIs are a subset of the old:
            Arguments.of("existing-app-3.json", "app-to-deploy-3.json", Arrays.asList("tcp://test.domain.com:51052", "tcp://test.domain.com:51054")),
            // (4) There is no previous version of app:
            Arguments.of(null, "app-to-deploy-3.json", Collections.emptyList())
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testExecute(String existingAppFile, String appToDeployFile, List<String> urisToDelete) {
        prepareContext(existingAppFile, appToDeployFile, urisToDelete);

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

    private void prepareContext(String existingAppFile, String appToDeployFile, List<String> urisToDelete) {
        StepsUtil.setDeleteIdleUris(context, true);
        
        if (existingAppFile == null) {
            StepsUtil.setExistingApp(context, null);
        } else {
            CloudApplicationExtended existingApp = JsonUtil.fromJson(TestUtil.getResourceAsString(existingAppFile, getClass()),
                new TypeReference<CloudApplicationExtended>() {
                });
            StepsUtil.setExistingApp(context, existingApp);            
        }
        
        CloudApplicationExtended appToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(appToDeployFile, getClass()),
            new TypeReference<CloudApplicationExtended>() {
            });

        StepsUtil.setApp(context, appToDeploy);
    }

    @Override
    protected DeleteIdleRoutesStep createStep() {
        return new DeleteIdleRoutesStep();
    }

}
