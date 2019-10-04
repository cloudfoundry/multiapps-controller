package com.sap.cloud.lm.sl.cf.process.analytics.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class UndeployProcessAttributes extends AbstractCommonProcessAttributes {

    public UndeployProcessAttributes() {

    }

    public UndeployProcessAttributes(Integer subscriptionsToDelete, Integer deletedEntries, Integer appsToUndeploy,
                                     Integer servicesToDelete, Integer updatedSubscribers, Integer updatedServiceBrokerSubscribers) {
        super(subscriptionsToDelete, deletedEntries, appsToUndeploy, servicesToDelete, updatedSubscribers, updatedServiceBrokerSubscribers);
    }

}
