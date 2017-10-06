package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Target;
import com.sap.cloud.lm.sl.mta.model.v1_0.Platform;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class OrgAndSpaceHelper {

    public static final String PROP_ORG = "org";
    public static final String PROP_SPACE = "space";

    protected Target target;
    protected Platform platform;
    protected DeploymentDescriptor descriptor;

    public OrgAndSpaceHelper(Target target, Platform platform) {
        this.target = target;
        this.platform = platform;
    }

    public OrgAndSpaceHelper(DeploymentDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    protected Pair<String, String> getOrgAndSpace(List<Map<String, Object>> propertiesList) {
        return new Pair<>((String) getPropertyValue(propertiesList, PROP_ORG, null),
            (String) getPropertyValue(propertiesList, PROP_SPACE, null));
    }

    public Pair<String, String> getOrgAndSpace() {
        return getOrgAndSpace(PropertiesUtil.getPropertiesList(target, platform, descriptor));
    }

}
