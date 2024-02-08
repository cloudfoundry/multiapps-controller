package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

public final class OperationFileIdsUtil {

    private OperationFileIdsUtil() {

    }

    public static List<String> getOperationFileIds(DelegateExecution execution) {
        String appArchiveId = VariableHandling.get(execution, Variables.APP_ARCHIVE_ID);
        String extensionDescriptorId = VariableHandling.get(execution, Variables.EXT_DESCRIPTOR_FILE_ID);
        String[] appArchiveIds = appArchiveId != null ? appArchiveId.split(",") : new String[0];
        String[] extensionDescriptorIds = extensionDescriptorId != null ? extensionDescriptorId.split(",") : new String[0];
        return List.of(ArrayUtils.addAll(appArchiveIds, extensionDescriptorIds));
    }

}
