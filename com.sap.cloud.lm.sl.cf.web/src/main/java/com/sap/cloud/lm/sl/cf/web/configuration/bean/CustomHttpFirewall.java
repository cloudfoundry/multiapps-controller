package com.sap.cloud.lm.sl.cf.web.configuration.bean;

import javax.inject.Named;

import org.springframework.security.web.firewall.DefaultHttpFirewall;

@Named("customHttpFirewall")
public class CustomHttpFirewall extends DefaultHttpFirewall {

    public CustomHttpFirewall() {
        setAllowUrlEncodedSlash(true);
    }

}
