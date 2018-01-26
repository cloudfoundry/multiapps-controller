package com.sap.cloud.lm.sl.cf.web.init;

import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.stereotype.Component;

@Component("customHttpFirewall")
public class CustomHttpFirewall extends DefaultHttpFirewall {
    public CustomHttpFirewall() {
        setAllowUrlEncodedSlash(true);
    }
}
