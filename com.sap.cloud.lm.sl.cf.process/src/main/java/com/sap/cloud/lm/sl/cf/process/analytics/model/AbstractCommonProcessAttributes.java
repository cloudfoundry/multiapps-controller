package com.sap.cloud.lm.sl.cf.process.analytics.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

@XmlRootElement(name = "processSpecificAttributes")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({ DeployProcessAttributes.class, UndeployProcessAttributes.class })
public abstract class AbstractCommonProcessAttributes {

    @XmlElement
    private Integer subscriptionsToDelete;

    @XmlElement
    private Integer deletedEntries;

    @XmlElement
    private Integer appsToUndeploy;

    @XmlElement
    private Integer servicesToDelete;

    @XmlElement
    private Integer updatedSubscribers;

    @XmlElement
    private Integer updatedServiceBrokerSubscribers;

    public AbstractCommonProcessAttributes() {

    }

    public AbstractCommonProcessAttributes(Integer subscriptionsToDelete, Integer deletedEntries, Integer appsToUndeploy,
        Integer servicesToDelete, Integer updatedSubscribers, Integer updatedServiceBrokerSubscribers) {
        super();
        this.subscriptionsToDelete = subscriptionsToDelete;
        this.deletedEntries = deletedEntries;
        this.appsToUndeploy = appsToUndeploy;
        this.servicesToDelete = servicesToDelete;
        this.updatedSubscribers = updatedSubscribers;
        this.updatedServiceBrokerSubscribers = updatedServiceBrokerSubscribers;
    }

    public Integer getSubscriptionsToDelete() {
        return subscriptionsToDelete;
    }

    public void setSubscriptionsToDelete(Integer subscriptionsToDelete) {
        this.subscriptionsToDelete = subscriptionsToDelete;
    }

    public Integer getDeletedEntries() {
        return deletedEntries;
    }

    public void setDeletedEntries(Integer deletedEntries) {
        this.deletedEntries = deletedEntries;
    }

    public Integer getAppsToUndeploy() {
        return appsToUndeploy;
    }

    public void setAppsToUndeploy(Integer appsToUndeploy) {
        this.appsToUndeploy = appsToUndeploy;
    }

    public Integer getServicesToDelete() {
        return servicesToDelete;
    }

    public void setServicesToDelete(Integer servicesToDelete) {
        this.servicesToDelete = servicesToDelete;
    }

    public Integer getUpdatedSubscribers() {
        return updatedSubscribers;
    }

    public void setUpdatedSubscripers(Integer updatedSubscribers) {
        this.updatedSubscribers = updatedSubscribers;
    }

    public Integer getUpdatedServiceBrokerSubscribers() {
        return updatedServiceBrokerSubscribers;
    }

    public void setUpdatedServiceBrokerSubscribers(Integer updatedServiceBrokerSubscribers) {
        this.updatedServiceBrokerSubscribers = updatedServiceBrokerSubscribers;
    }

}
