package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.Map;

public class PaginationV3 extends ResponseUrisV3 {

    private PaginationV3(Map<String, Object> includedUris) {
        super(includedUris);
    }

    public static PaginationV3 fromResponse(Map<String, Object> responseMap) {
        return new PaginationV3((Map<String, Object>) responseMap.get("pagination"));
    }

    public String getFirstUri() {
        return getUriString("first");
    }

    public String getLastUri() {
        return getUriString("last");
    }

    public String getNextUri() {
        return getUriString("next");
    }

    public String getPreviousUri() {
        return getUriString("previous");
    }
}
