package org.cloudfoundry.multiapps.controller.core.cf.metadata.processor;

import java.text.MessageFormat;
import java.util.Map;

import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;

@Named
public class EnvMtaMetadataValidator {

    public void validate(CloudApplication application) {
        validateMtaMetadataIsPresent(application);
        validateMtaMetadataStructure(application);
        validateMtaModuleMetadataStructure(application);
    }

    private void validateMtaMetadataIsPresent(CloudApplication application) {
        if (!application.getEnv()
                        .keySet()
                        .containsAll(MtaMetadataUtil.ENV_MTA_METADATA_FIELDS)) {
            throw new ContentException(Messages.MTA_METADATA_FOR_APP_0_IS_INCOMPLETE, application.getName());
        }
    }

    private void validateMtaMetadataStructure(CloudApplication application) {
        String mtaMetadataEnv = application.getEnv()
                                           .get(Constants.ENV_MTA_METADATA);
        Map<String, Object> mtaMetadata = JsonUtil.convertJsonToMap(mtaMetadataEnv);
        String exceptionMessage = getInvalidValueInMetadataMessage(application, Constants.ENV_MTA_METADATA);
        validateContainsKey(mtaMetadata, Constants.ATTR_ID, exceptionMessage);
        validateContainsKey(mtaMetadata, Constants.ATTR_VERSION, exceptionMessage);
    }

    private String getInvalidValueInMetadataMessage(CloudApplication application, String field) {
        return MessageFormat.format(Messages.ENV_OF_APP_0_CONTAINS_INVALID_VALUE_FOR_1, application.getName(), field);
    }

    private void validateContainsKey(Map<String, Object> map, String requiredKey, String exceptionMessage) {
        if (!map.containsKey(requiredKey)) {
            throw new ContentException(exceptionMessage);
        }
    }

    private void validateMtaModuleMetadataStructure(CloudApplication application) {
        String mtaModuleMetadataEnv = application.getEnv()
                                                 .get(Constants.ENV_MTA_MODULE_METADATA);
        Map<String, Object> mtaModuleMetadata = JsonUtil.convertJsonToMap(mtaModuleMetadataEnv);
        String exceptionMessage = getInvalidValueInMetadataMessage(application, Constants.ENV_MTA_MODULE_METADATA);
        validateContainsKey(mtaModuleMetadata, Constants.ATTR_NAME, exceptionMessage);
    }

}
