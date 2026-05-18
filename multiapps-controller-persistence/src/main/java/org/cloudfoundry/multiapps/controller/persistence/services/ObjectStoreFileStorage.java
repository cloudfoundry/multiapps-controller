package org.cloudfoundry.multiapps.controller.persistence.services;

import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.springframework.beans.factory.DisposableBean;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class ObjectStoreFileStorage implements FileStorage, DisposableBean {

    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public List<FileEntry> getExistingFileEntries(List<FileEntry> fileEntries) {
        if (fileEntries.isEmpty()) {
            return List.of();
        }
        List<CompletableFuture<FileEntry>> existenceChecks = fileEntries.stream()
                                                                        .map(this::asyncCheckExistenceOfFileEntry)
                                                                        .toList();
        return existenceChecks.stream()
                              .map(CompletableFuture::join)
                              .filter(Objects::nonNull)
                              .toList();
    }

    private CompletableFuture<FileEntry> asyncCheckExistenceOfFileEntry(FileEntry fileEntry) {
        return CompletableFuture.supplyAsync(() -> toFileEntryIfExists(fileEntry), virtualThreadExecutor);
    }

    private FileEntry toFileEntryIfExists(FileEntry fileEntry) {
        if (existsInObjectStore(fileEntry)) {
            return fileEntry;
        }
        return null;
    }

    protected abstract boolean existsInObjectStore(FileEntry fileEntry);

    @Override
    public void destroy() {
        virtualThreadExecutor.shutdown();
    }
}
