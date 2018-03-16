package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.activiti.engine.delegate.DelegateExecution;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudTask;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;

public class StepsTestUtil {

    private static final String COULD_NOT_LOAD_PLATFORM_TYPES = "Could not load test platform types: {0}";
    private static final String COULD_NOT_LOAD_PLATFORMS = "Could not load test platforms: {0}";
    private static final String COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR = "Could not load test deployment descriptor: {0}";

    public static List<Platform> loadPlatforms(ConfigurationParser parser, String location, Class<?> testClass) {
        try {
            return parser.parsePlatformsJson(TestUtil.getResourceAsString(location, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_PLATFORM_TYPES, e.getMessage()));
            return null;
        }
    }

    public static DeploymentDescriptor loadDeploymentDescriptor(DescriptorParser parser, String location, Class<?> testClass) {
        try {
            return parser.parseDeploymentDescriptorYaml(TestUtil.getResourceAsString(location, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR, e.getMessage()));
            return null;
        }
    }

    public static List<Target> loadTargets(ConfigurationParser parser, String location, Class<?> testClass) {
        try {
            return parser.parseTargetsJson(TestUtil.getResourceAsString(location, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_PLATFORMS, e.getMessage()));
            return null;
        }
    }

    public static void mockApplicationsToDeploy(List<CloudApplicationExtended> applications, DelegateExecution context) {
        String[] appsInArray = getAppsInArray(applications);
        for (String appInArray : appsInArray) {
            // FIXME: This does not work! It will always return the last app in the array.
            Mockito.when(context.getVariable(Constants.VAR_APP_TO_DEPLOY))
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
