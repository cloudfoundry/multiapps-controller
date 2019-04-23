package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudTask;
import org.flowable.engine.delegate.DelegateExecution;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Platform;
import com.sap.cloud.lm.sl.mta.model.v2.ExtensionDescriptor;

public class StepsTestUtil {

    private static final String COULD_NOT_LOAD_PLATFORM = "Could not load test platform: {0}";
    private static final String COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR = "Could not load test deployment descriptor: {0}";
    private static final DescriptorParserFacade DESCRIPTOR_PARSER_FACADE = new DescriptorParserFacade();

    public static Platform loadPlatform(String filePath, Class<?> testClass) {
        try {
            return new ConfigurationParser().parsePlatformJson(TestUtil.getResourceAsString(filePath, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_PLATFORM, e.getMessage()));
            return null;
        }
    }

    public static DeploymentDescriptor loadDeploymentDescriptor(String filePath, Class<?> testClass) {
        try {
            return DESCRIPTOR_PARSER_FACADE.parseDeploymentDescriptor(TestUtil.getResourceAsString(filePath, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR, e.getMessage()));
            return null;
        }
    }

    public static ExtensionDescriptor loadExtensionDescriptor(String filePath, Class<?> testClass) {
        try {
            return DESCRIPTOR_PARSER_FACADE.parseExtensionDescriptor(TestUtil.getResourceAsString(filePath, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR, e.getMessage()));
            return null;
        }
    }

    public static void mockApplicationsToDeploy(List<CloudApplicationExtended> applications, DelegateExecution context) {
        String[] appsInArray = getAppsInArray(applications);
        for (String appInArray : appsInArray) {
            // FIXME: This does not work! It will always return the last app in the array.
            Mockito.when(context.getVariable(Constants.VAR_APP_TO_PROCESS))
                .thenReturn(appInArray);
        }
    }

    public static CloudTask copy(CloudTask task) {
        CloudTask copy = new CloudTask(null, null);
        copy.setCommand(task.getCommand());
        copy.setState(task.getState());
        copy.setMeta(task.getMeta());
        copy.setName(task.getName());
        copy.setEnvironmentVariables(task.getEnvironmentVariables());
        copy.setResult(task.getResult());
        return copy;
    }

    private static String[] getAppsInArray(List<CloudApplicationExtended> applications) {
        List<String> applicationsString = new ArrayList<>();
        for (CloudApplicationExtended app : applications) {
            applicationsString.add(JsonUtil.toJson(app));
        }
        return applicationsString.toArray(new String[0]);
    }

}
