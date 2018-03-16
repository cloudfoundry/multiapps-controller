package com.sap.cloud.lm.sl.cf.core.helpers;

import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;

public class MtaPathValidator {

    public static void validatePath(String path) throws ContentException {

        if (!path.equals(FilenameUtils.separatorsToUnix(path))) {
            throw new ContentException(Messages.PATH_MUST_NOT_CONTAIN_WINDOWS_SEPARATORS, path);
        }
        if (Paths.get(path)
            .isAbsolute()) {
            throw new ContentException(Messages.PATH_SHOULD_NOT_BE_ABSOLUTE, path);
        }
        if (!path.equals(FilenameUtils.normalize(path, true))) {
            throw new ContentException(Messages.PATH_SHOULD_BE_NORMALIZED, path);
        }
    }
}
