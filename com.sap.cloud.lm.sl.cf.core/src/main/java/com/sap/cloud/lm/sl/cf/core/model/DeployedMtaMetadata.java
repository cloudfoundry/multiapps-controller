package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Objects;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloud.lm.sl.cf.core.model.adapter.VersionJsonDeserializer;
import com.sap.cloud.lm.sl.cf.core.model.adapter.VersionJsonSerializer;
import com.sap.cloud.lm.sl.cf.core.model.adapter.VersionXmlAdapter;
import com.sap.cloud.lm.sl.mta.model.Version;

public class DeployedMtaMetadata {

    // In order to keep backwards compatibility the version element cannot be null, since old clients might throw a NPE. TODO: Remove this
    // when compatibility with versions lower than 1.27.3 is not required.
    private static final Version UNKNOWN_MTA_VERSION = Version.parseVersion("0.0.0-unknown");

    private String id;
    @JsonSerialize(using = VersionJsonSerializer.class)
    @JsonDeserialize(using = VersionJsonDeserializer.class)
    @XmlJavaTypeAdapter(VersionXmlAdapter.class)
    private Version version;

    public DeployedMtaMetadata() {
    }

    public DeployedMtaMetadata(String id) {
        this(id, UNKNOWN_MTA_VERSION);
    }

    public DeployedMtaMetadata(String id, Version version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public Version getVersion() {
        return version;
    }

    public boolean isVersionUnknown() {
        return version.equals(UNKNOWN_MTA_VERSION);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        DeployedMtaMetadata other = (DeployedMtaMetadata) object;
        return Objects.equals(id, other.id) && Objects.equals(version, other.version);
    }

}
