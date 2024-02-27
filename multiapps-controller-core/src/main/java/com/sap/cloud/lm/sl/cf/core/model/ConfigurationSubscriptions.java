package com.sap.cloud.lm.sl.cf.core.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "configuration-subscriptions")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConfigurationSubscriptions {

    @XmlElement(name = "configuration-subscription")
    private List<ConfigurationSubscription> configurationSubscriptions = new ArrayList<>();

    public ConfigurationSubscriptions() {
        // Required by JaxB
    }

    public ConfigurationSubscriptions(List<ConfigurationSubscription> configurationSubscriptions) {
        this.configurationSubscriptions = configurationSubscriptions;
    }

    public List<ConfigurationSubscription> getConfigurationSubscriptions() {
        return configurationSubscriptions;
    }

}
