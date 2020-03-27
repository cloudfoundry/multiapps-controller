package com.sap.cloud.lm.sl.cf.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import com.sap.cloud.lm.sl.cf.core.Messages;

public class FileUtils {

    public static final String PATH_SHOULD_NOT_BE_ABSOLUTE = "Archive entry name \"{0}\" should not be absolute";
    public static final String PATH_SHOULD_BE_NORMALIZED = "Archive entry name \"{0}\" should be normalized";

    private FileUtils() {
    }

    public static void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new DeleteDirVisitor());
    }

    public static void copyDirectory(Path fromPath, Path toPath) throws IOException {
        Files.walkFileTree(fromPath, new CopyDirVisitor(fromPath, toPath));
    }

    public static void copyFile(Path fromPath, Path toPath) throws IOException {
        Path destinationParent = toPath.getParent();
        if (!destinationParent.toFile()
                              .exists()) {
            Files.createDirectories(destinationParent);
        }
        Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    public static void validatePath(String path) {
        if (!path.equals(FilenameUtils.normalize(path, true))) {
            throw new IllegalArgumentException(MessageFormat.format(PATH_SHOULD_BE_NORMALIZED, path));
        }
        if (Paths.get(path)
                 .isAbsolute()) {
            throw new IllegalArgumentException(MessageFormat.format(PATH_SHOULD_NOT_BE_ABSOLUTE, path));
        }
    }

    public static String getRelativePath(String parentPath, String filePath) {
        return Paths.get(parentPath)
                    .relativize(Paths.get(filePath))
                    .toString();
    }

    public static boolean isDirectory(String fileName) {
        return fileName.endsWith("/");
    }

    public static void cleanUp(Path filePath, Logger logger) {
        if (filePath == null) {
            return;
        }
        File file = filePath.toFile();
        if (!file.exists()) {
            return;
        }
        try {
            logger.debug(Messages.DELETING_TEMP_FILE, filePath);
            org.apache.commons.io.FileUtils.forceDelete(file);
        } catch (IOException e) {
            logger.warn(Messages.ERROR_DELETING_APP_TEMP_FILE, filePath.toAbsolutePath());
        }
    }

    private static class DeleteDirVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            if (exc == null) {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
            throw exc;
        }
    }

    private static class CopyDirVisitor extends SimpleFileVisitor<Path> {

        private final Path fromPath;
        private final Path toPath;

        CopyDirVisitor(Path fromPath, Path toPath) {
            this.fromPath = fromPath;
            this.toPath = toPath;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            Path targetPath = toPath.resolve(fromPath.relativize(dir));
            if (!targetPath.toFile()
                           .exists()) {
                Files.createDirectory(targetPath);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, toPath.resolve(fromPath.relativize(file)), StandardCopyOption.REPLACE_EXISTING,
                       StandardCopyOption.COPY_ATTRIBUTES);
            return FileVisitResult.CONTINUE;
        }
    }
}
