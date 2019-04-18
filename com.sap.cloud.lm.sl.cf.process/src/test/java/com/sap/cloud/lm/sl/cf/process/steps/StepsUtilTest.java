package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.UploadToken;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.model.Phase;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;

public class StepsUtilTest {

    protected DelegateExecution context = MockDelegateExecution.createSpyInstance();

    @Test
    public void testGetServicesToCreateWithCredentials() throws Exception {
        Map<String, Object> credentials = new HashMap<String, Object>();
        credentials.put("integer-value", (Integer) 1);
        credentials.put("int-double-value", (Double) 1.0);
        credentials.put("real-double-value", (Double) 1.4);
        credentials.put("string-value", (String) "1");
        CloudServiceExtended service = new CloudServiceExtended(null, "my-service");
        service.setCredentials(credentials);

        StepsUtil.setServicesToCreate(context, Arrays.asList(service));
        List<CloudServiceExtended> actualServicesToCreate = StepsUtil.getServicesToCreate(context);

        assertEquals(1, actualServicesToCreate.size());
        assertTrue(!actualServicesToCreate.get(0)
            .getCredentials()
            .isEmpty());
        assertEquals(Integer.class, actualServicesToCreate.get(0)
            .getCredentials()
            .get("integer-value")
            .getClass());
        assertEquals(Integer.class, actualServicesToCreate.get(0)
            .getCredentials()
            .get("int-double-value")
            .getClass());
        assertEquals(Double.class, actualServicesToCreate.get(0)
            .getCredentials()
            .get("real-double-value")
            .getClass());
        assertEquals(String.class, actualServicesToCreate.get(0)
            .getCredentials()
            .get("string-value")
            .getClass());
    }

    @Test
    public void testGetAppsToDeployWithBindingParameters() throws Exception {
        Map<String, Map<String, Object>> bindingParameters = new HashMap<String, Map<String, Object>>();
        Map<String, Object> serviceBindingParameters = new HashMap<String, Object>();
        serviceBindingParameters.put("integer-value", (Integer) 1);
        serviceBindingParameters.put("int-double-value", (Double) 1.0);
        serviceBindingParameters.put("real-double-value", (Double) 1.4);
        serviceBindingParameters.put("string-value", (String) "1");
        bindingParameters.put("service-1", serviceBindingParameters);
        CloudApplicationExtended application = new CloudApplicationExtended(null, "my-app");
        application.setBindingParameters(bindingParameters);

        StepsUtil.setApp(context, application);
        CloudApplicationExtended actualAppToDeploy = StepsUtil.getApp(context);

        assertTrue(!actualAppToDeploy.getBindingParameters()
            .isEmpty());
        assertTrue(!actualAppToDeploy.getBindingParameters()
            .get("service-1")
            .isEmpty());
        assertEquals(Integer.class, actualAppToDeploy.getBindingParameters()
            .get("service-1")
            .get("integer-value")
            .getClass());
        assertEquals(Integer.class, actualAppToDeploy.getBindingParameters()
            .get("service-1")
            .get("int-double-value")
            .getClass());
        assertEquals(Double.class, actualAppToDeploy.getBindingParameters()
            .get("service-1")
            .get("real-double-value")
            .getClass());
        assertEquals(String.class, actualAppToDeploy.getBindingParameters()
            .get("service-1")
            .get("string-value")
            .getClass());
    }

    @Test
    public void testSetAndGetUploadToken() {
        UploadToken expectedUploadToken = new UploadToken();
        expectedUploadToken.setPackageGuid(UUID.fromString("ab0703c2-1a50-11e9-ab14-d663bd873d93"));
        expectedUploadToken.setToken("token");

        StepsUtil.setUploadToken(expectedUploadToken, context);
        UploadToken actualUploadToken = StepsUtil.getUploadToken(context);

        assertEquals(expectedUploadToken.getToken(), actualUploadToken.getToken());
        assertEquals(expectedUploadToken.getPackageGuid(), actualUploadToken.getPackageGuid());
    }

    @Test
    public void testSetAndGetPhase() {
        Phase expectedPhase = Phase.UNDEPLOY;
        StepsUtil.setPhase(context, expectedPhase);
        Phase actualPhase = Phase.valueOf((String) context.getVariable(Constants.VAR_PHASE));

        assertEquals(expectedPhase, actualPhase);
    }

}
