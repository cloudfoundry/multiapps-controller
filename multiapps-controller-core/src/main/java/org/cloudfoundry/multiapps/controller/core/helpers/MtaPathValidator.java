package org.cloudfoundry.multiapps.controller.core.helpers;

import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.Messages;

public class MtaPathValidator {

    private MtaPathValidator() {
    }

    public static void validatePath(String path) {
        if (containsWindowsSeparators(path)) {
            throw new ContentException(Messages.PATH_MUST_NOT_CONTAIN_WINDOWS_SEPARATORS, path);
        }
        if (isAbsolute(path)) {
            throw new ContentException(Messages.PATH_SHOULD_NOT_BE_ABSOLUTE, path);
        }
        if (!isNormalized(path)) {
            throw new ContentException(Messages.PATH_SHOULD_BE_NORMALIZED, path);
        }
    }

    private static boolean containsWindowsSeparators(String path) {
        String pathWithUnixSeparators = FilenameUtils.separatorsToUnix(path);
        return !path.equals(pathWithUnixSeparators);
    }

    private static boolean isAbsolute(String path) {
        return FilenameUtils.getPrefixLength(path) != 0;
    }

    private static boolean isNormalized(String path) {
        String normalizedPath = FilenameUtils.normalize(path, true);
        return path.equals(normalizedPath);
    }

}
