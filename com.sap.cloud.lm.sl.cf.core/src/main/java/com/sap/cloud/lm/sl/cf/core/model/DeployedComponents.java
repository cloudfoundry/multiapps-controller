package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(value = javax.xml.bind.annotation.XmlAccessType.FIELD)
@XmlRootElement(name = "components")
public class DeployedComponents {

    @XmlElementWrapper(name = "mtas")
    @XmlElement(name = "mta")
    private List<DeployedMta> mtas;

    @XmlElementWrapper(name = "standaloneApps")
    @XmlElement(name = "standaloneApp")
    private List<String> standaloneApps;

    public DeployedComponents() {
        // Required by JAXB
    }

    public DeployedComponents(List<DeployedMta> mtas, List<String> standaloneApps) {
        this.mtas = mtas;
        this.standaloneApps = standaloneApps;
    }

    public List<DeployedMta> getMtas() {
        return mtas;
    }

    public void setMtas(List<DeployedMta> mtas) {
        this.mtas = mtas;
    }

    public List<String> getStandaloneApps() {
        return standaloneApps;
    }

    public void setStandaloneApps(List<String> standaloneApps) {
        this.standaloneApps = standaloneApps;
    }

    public DeployedMta findDeployedMta(String mtaId) {
        return getMtas().stream().filter(mta -> mta.getMetadata().getId().equalsIgnoreCase(mtaId)).findFirst().orElse(null);
    }

}
