package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.text.MessageFormat;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;

public class SpaceGetterFactory {

    public SpaceGetter createSpaceGetter() {
        PlatformType platformType = Configuration.getInstance().getPlatformType();
        switch (platformType) {
            case XS2:
                // TODO: Implement XSOptimizedSpaceGetter if necessary. For now, we haven't had any performance issues related to the
                // getSpaces() method on XS (probably because there usually aren't that many spaces on it).
                return new SpaceGetter();
            case CF:
                return new CFOptimizedSpaceGetter();
            default:
                throw new IllegalStateException(MessageFormat.format("Unknown platform type: {0}", platformType));
        }
    }

}
