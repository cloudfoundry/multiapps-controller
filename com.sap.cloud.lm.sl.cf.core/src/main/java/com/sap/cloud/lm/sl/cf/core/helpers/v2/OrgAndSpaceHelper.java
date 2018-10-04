package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;
import com.sap.cloud.lm.sl.mta.model.v2.Target;
import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class OrgAndSpaceHelper extends com.sap.cloud.lm.sl.cf.core.helpers.v1.OrgAndSpaceHelper {

    public OrgAndSpaceHelper(DeploymentDescriptor descriptor) {
        super(descriptor);
    }

    public OrgAndSpaceHelper(Target target, Platform platform) {
        super(target, platform);
    }

    @Override
    public Pair<String, String> getOrgAndSpace() {
        List<Map<String, Object>> parameters = PropertiesUtil.getParametersList((com.sap.cloud.lm.sl.mta.model.v2.Target) target,
            (com.sap.cloud.lm.sl.mta.model.v2.Platform) platform);
        return getOrgAndSpace(parameters);
    }

}
