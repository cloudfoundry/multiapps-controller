package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationFileDigestDetector {

    private final Map<String, String> appEnv;

    public ApplicationFileDigestDetector(Map<String, String> appEnv) {
        this.appEnv = appEnv;
    }

    public String detectCurrentAppFileDigest() {
        try {
            Map<String, Object> applicationDeployAttributes = getApplicationDeployAttributes();
            return MapUtils.getString(applicationDeployAttributes, Constants.ATTR_APP_CONTENT_DIGEST);
        } catch (ParsingException e) {
            return null;
        }
    }

    private Map<String, Object> getApplicationDeployAttributes() {
        String applicationDeployAttributes = appEnv.get(Constants.ENV_DEPLOY_ATTRIBUTES);
        return JsonUtil.convertJsonToMap(applicationDeployAttributes);
    }

}
