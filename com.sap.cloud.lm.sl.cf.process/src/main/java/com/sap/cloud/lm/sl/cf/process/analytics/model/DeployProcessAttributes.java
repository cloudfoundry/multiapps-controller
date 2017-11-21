package com.sap.cloud.lm.sl.cf.process.analytics.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "processSpecificAttributes")
@XmlAccessorType(XmlAccessType.FIELD)
public class DeployProcessAttributes extends AbstractCommonProcessAttributes {

    @XmlElement
    private Integer mtaSize;

    @XmlElement
    private Integer customDomains;

    @XmlElement
    private Integer servicesToCreate;

    @XmlElement
    private Integer appsToDeploy;

    @XmlElement
    private Integer publishedEntries;

    @XmlElement
    private Integer subscriptionsToCreate;

    @XmlElement
    private Integer serviceUrlsToRegister;

    @XmlElement
    private Integer serviceBrokersToCreate;

    @XmlElement
    private Integer triggeredServiceOperations;

    @XmlElement
    private Integer serviceKeysToCreate;

    public DeployProcessAttributes(Integer subscriptionsToDelete, Integer deletedEntries, Integer appsToUndeploy, Integer servicesToDelete,
        Integer updatedSubscribers, Integer updatedServiceBrokerSubscribers, Integer mtaSize, Integer customDomains,
        Integer servicesToCreate, Integer appsToDeploy, Integer publishedEntries, Integer subscriptionsToCreate,
        Integer serviceUrlsToRegister, Integer serviceBrokersToCreate, Integer triggeredServiceOperations, Integer serviceKeysToCreate) {
        super(subscriptionsToDelete, deletedEntries, appsToUndeploy, servicesToDelete, updatedSubscribers, updatedServiceBrokerSubscribers);
        setMtaSize(mtaSize);
        setCustomDomains(customDomains);
        setServicesToCreate(servicesToCreate);
        setAppsToDeploy(appsToDeploy);
        setPublishedEntries(publishedEntries);
        setSubscriptionsToCreate(subscriptionsToCreate);
        setServiceUrlsToRegister(serviceUrlsToRegister);
        setServiceBrokersToCreate(serviceBrokersToCreate);
        setTriggeredServiceOperations(triggeredServiceOperations);
        setServiceKeysToCreate(serviceKeysToCreate);
    }

    public DeployProcessAttributes() {
        // TODO Auto-generated constructor stub
    }

    public Integer getMtaSize() {
        return mtaSize;
    }

    public void setMtaSize(Integer mtaSize) {
        this.mtaSize = mtaSize;
    }

    public Integer getCustomDomains() {
        return customDomains;
    }

    public void setCustomDomains(Integer customDomains) {
        this.customDomains = customDomains;
    }

    public Integer getServicesToCreate() {
        return servicesToCreate;
    }

    public void setServicesToCreate(Integer servicesToCreate) {
        this.servicesToCreate = servicesToCreate;
    }

    public Integer getAppsToDeploy() {
        return appsToDeploy;
    }

    public void setAppsToDeploy(Integer appsToDeploy) {
        this.appsToDeploy = appsToDeploy;
    }

    public Integer getPublishedEntries() {
        return publishedEntries;
    }

    public void setPublishedEntries(Integer publishedEntries) {
        this.publishedEntries = publishedEntries;
    }

    public Integer getSubscriptionsToCreate() {
        return subscriptionsToCreate;
    }

    public void setSubscriptionsToCreate(Integer subscriptionsToCreate) {
        this.subscriptionsToCreate = subscriptionsToCreate;
    }

    public Integer getServiceUrlsToRegister() {
        return serviceUrlsToRegister;
    }

    public void setServiceUrlsToRegister(Integer serviceUrlsToRegister) {
        this.serviceUrlsToRegister = serviceUrlsToRegister;
    }

    public Integer getServiceBrokersToCreate() {
        return serviceBrokersToCreate;
    }

    public void setServiceBrokersToCreate(Integer serviceBrokersToCreate) {
        this.serviceBrokersToCreate = serviceBrokersToCreate;
    }

    public Integer getTriggeredServiceOperations() {
        return triggeredServiceOperations;
    }

    public void setTriggeredServiceOperations(Integer triggeredServiceOperations) {
        this.triggeredServiceOperations = triggeredServiceOperations;
    }

    public Integer getServiceKeysToCreate() {
        return serviceKeysToCreate;
    }

    public void setServiceKeysToCreate(Integer serviceKeysToCreate) {
        this.serviceKeysToCreate = serviceKeysToCreate;
    }

}
