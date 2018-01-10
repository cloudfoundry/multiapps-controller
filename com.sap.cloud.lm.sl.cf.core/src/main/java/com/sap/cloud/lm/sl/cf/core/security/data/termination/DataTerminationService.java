package com.sap.cloud.lm.sl.cf.core.security.data.termination;

import java.util.List;
import javax.inject.Inject;
import org.springframework.stereotype.Component;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;

@Component
public class DataTerminationService {
    
    @Inject
    private ConfigurationEntryDao entryDao;

    @Inject
    private ConfigurationSubscriptionDao subscriptionDao;
    
    
    public void deleteOrphanData(List<String> spaceIds){
        
        for(String spaceId: spaceIds){
            
            deleteConfigurationSubscriptionOrphanData(spaceId);
            deleteConfigurationEntryOrphanData(spaceId);
        }
    }
    
    private void deleteConfigurationSubscriptionOrphanData(String space_guid){
        List<ConfigurationSubscription> configurationSubscriptions = subscriptionDao.findAll(null, null, space_guid, null);
        subscriptionDao.removeAll(configurationSubscriptions);
    }
    
    private void deleteConfigurationEntryOrphanData(String space_guid){
    }
}
