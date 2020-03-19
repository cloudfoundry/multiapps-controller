package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import javax.inject.Named;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;

@Named("deleteRemainingFileParts")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteRemainingFileParts extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        List<FileEntry> filesToRemove = StepsUtil.getFromJsonBinaries(context.getExecution(), Constants.VAR_FILE_ENTRIES, FileEntry.class);
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
