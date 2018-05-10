package com.sap.cloud.lm.sl.cf.client.uaa;

import java.net.URL;

import org.springframework.web.client.RestTemplate;

public class UAAClientFactory {

    public UAAClient createClient(URL uaaUrl) {
        return new UAAClient(uaaUrl, new RestTemplate());
    }

}
