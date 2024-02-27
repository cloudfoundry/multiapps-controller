package com.sap.cloud.lm.sl.cf.core.model;

import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.ORG;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.SPACE;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.gson.annotations.Expose;
import com.sap.cloud.lm.sl.cf.core.filters.TargetWildcardFilter;

@XmlRootElement(name = "target")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class CloudTarget {

    @Expose
    @XmlElement(name = "space")
    @QueryParam(SPACE)
    @DefaultValue(TargetWildcardFilter.ANY_TARGET_WILDCARD)
    private String space;

    @Expose
    @XmlElement(name = "org")
    @QueryParam(ORG)
    @DefaultValue(TargetWildcardFilter.ANY_TARGET_WILDCARD)
    private String org;

    public CloudTarget() {
        // Required by JAXB.
    }

    public CloudTarget(String org, String space) {
        this.org = org;
        this.space = space;
    }

    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getOrg() {
        return org;
    }

    public void setOrg(String org) {
        this.org = org;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((org == null) ? 0 : org.hashCode());
        result = prime * result + ((space == null) ? 0 : space.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CloudTarget other = (CloudTarget) obj;
        if (org == null) {
            if (other.org != null)
                return false;
        } else if (!org.equals(other.org))
            return false;
        if (space == null) {
            if (other.space != null)
                return false;
        } else if (!space.equals(other.space))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CloudTarget [organizationName=" + org + ", spaceName=" + space + "]";
    }
}
