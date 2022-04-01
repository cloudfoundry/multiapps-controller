package org.cloudfoundry.multiapps.controller.core.cf.clients.v3;

import java.util.Map;

public class LinksV3 extends ResponseUrisV3 {

    private LinksV3(Map<String, Object> includedUris) {
        super(includedUris);
    }

    public static LinksV3 fromResponse(Map<String, Object> responseMap) {
        return new LinksV3((Map<String, Object>) responseMap.get("links"));
    }

    public String getSelfUri() {
        return getUriString("self");
    }
}
