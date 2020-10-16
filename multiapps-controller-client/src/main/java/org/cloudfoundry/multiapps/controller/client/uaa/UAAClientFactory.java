package org.cloudfoundry.multiapps.controller.client.uaa;

import java.net.URL;

import com.sap.cloudfoundry.client.facade.util.RestUtil;

public class UAAClientFactory {

    public UAAClient createClient(URL uaaUrl) {
        return new UAAClient(uaaUrl, new RestUtil().createWebClient(false));
    }

}
