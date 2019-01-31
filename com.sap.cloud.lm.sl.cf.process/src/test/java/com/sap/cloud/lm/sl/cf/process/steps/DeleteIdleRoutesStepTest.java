package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationURI;
import com.sap.cloud.lm.sl.cf.process.Constants;
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
            verify(client, never()).deleteRoute(anyString(), anyString(), anyString());
            return;
        }

        for (String uri : urisToDelete) {
            ApplicationURI parsedUri = new ApplicationURI(uri);
            if (parsedUri.isTcpOrTcps()) {
                assertTrue("The host segment should be a port number", NumberUtils.isDigits(parsedUri.getPort()));
                verify(client, times(1)).deleteRoute(parsedUri.getPort(), parsedUri.getDomain(), null);
            } else {
                verify(client, times(1)).deleteRoute(parsedUri.getHost(), parsedUri.getDomain(), null);
            }
        }
    }

    private void loadParameters(String appDetailsFile) {
        expectedAppToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(appDetailsFile, getClass()),
            new TypeToken<CloudApplicationExtended>() {
            }.getType());
        appToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(appDetailsFile, getClass()),
            new TypeToken<CloudApplicationExtended>() {
            }.getType());
    }

    private void prepareContext(List<String> urisToDelete) {
        context.setVariable(Constants.VAR_PORT_BASED_ROUTING, false);

        if (!urisToDelete.isEmpty()) {
            StepsUtil.setDeleteIdleUris(context, true);
        }

        CloudApplicationExtended existingApp = new CloudApplicationExtended(null, expectedAppToDeploy.getName());
        existingApp.setUris(urisToDelete);
        StepsUtil.setExistingApp(context, existingApp);

        StepsUtil.setApp(context, appToDeploy);
    }

    @Override
    protected DeleteIdleRoutesStep createStep() {
        return new DeleteIdleRoutesStep();
    }

}
