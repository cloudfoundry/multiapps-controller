package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatformType;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class OrgAndSpaceHelper extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.OrgAndSpaceHelper {

    public OrgAndSpaceHelper(DeploymentDescriptor descriptor) {
        super(descriptor);
    }

    public OrgAndSpaceHelper(TargetPlatform platform, TargetPlatformType platformType) {
        super(platform, platformType);
    }

    @Override
    public Pair<String, String> getOrgAndSpace() {
        List<Map<String, Object>> parameters = PropertiesUtil.getParametersList(
            (com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatform) platform,
            (com.sap.cloud.lm.sl.mta.model.v2_0.TargetPlatformType) platformType);
        return getOrgAndSpace(parameters);
    }

}
