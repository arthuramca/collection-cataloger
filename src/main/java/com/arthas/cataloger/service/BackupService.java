package com.arthas.cataloger.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupService {

    private static final String DB_PATH =
            System.getProperty("user.home") + "/collection-cataloger/catalog.db";

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public enum BackupResult {
        SUCCESS, NO_ONEDRIVE, DB_NOT_FOUND, ERROR
    }

    public BackupResult backupToOneDrive() {
        Path source = Paths.get(DB_PATH);
        if (!Files.exists(source)) return BackupResult.DB_NOT_FOUND;

        Path oneDrive = findOneDrivePath();
        if (oneDrive == null) return BackupResult.NO_ONEDRIVE;

        try {
            Path dest = oneDrive.resolve("collection-cataloger");
            Files.createDirectories(dest);
            String fileName = "catalog_" + LocalDateTime.now().format(TIMESTAMP) + ".db";
            Files.copy(source, dest.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return BackupResult.SUCCESS;
        } catch (IOException e) {
            return BackupResult.ERROR;
        }
    }

    public String backupToDirectory(File directory) throws IOException {
        Path source = Paths.get(DB_PATH);
        if (!Files.exists(source)) throw new IOException("Banco de dados não encontrado: " + DB_PATH);

        String fileName = "catalog_" + LocalDateTime.now().format(TIMESTAMP) + ".db";
        Path dest = directory.toPath().resolve(fileName);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.toString();
    }

    public Path findOneDrivePath() {
        String[] candidates = {
            System.getenv("OneDriveConsumer"),
            System.getenv("OneDrive"),
            System.getProperty("user.home") + "/OneDrive",
            System.getProperty("user.home") + "/OneDrive - Personal"
        };
        for (String c : candidates) {
            if (c != null && !c.isBlank() && Files.exists(Paths.get(c))) {
                return Paths.get(c);
            }
        }
        return null;
    }

    public boolean isOneDriveAvailable() {
        return findOneDrivePath() != null;
    }
}
