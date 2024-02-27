package com.sap.cloud.lm.sl.cf.core.model.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.sap.cloud.lm.sl.mta.model.Version;

public class VersionXmlAdapter extends XmlAdapter<String, Version> {

    @Override
    public String marshal(Version version) {
        return version.toString();
    }

    @Override
    public Version unmarshal(String versionString) {
        return Version.parseVersion(versionString);
    }

}
