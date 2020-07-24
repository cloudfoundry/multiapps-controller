package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("deleteRemainingFileParts")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteRemainingFileParts extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        List<FileEntry> filesToRemove = context.getVariable(Variables.FILE_ENTRIES);
        filesToRemove.forEach(this::attemptToDeleteFilePart);
        return StepPhase.DONE;
    }

    private void attemptToDeleteFilePart(FileEntry fileEntry) {
        try {
            fileService.deleteFile(fileEntry.getSpace(), fileEntry.getId());
        } catch (FileStorageException e) {
            logger.warn(Messages.ERROR_DELETING_ARCHIVE_PARTS_CONTENT, e);
        }
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_DELETING_REMAINING_FILE_PARTS;
    }
}
