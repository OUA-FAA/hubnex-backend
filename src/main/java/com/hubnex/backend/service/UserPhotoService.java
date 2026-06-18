package com.hubnex.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UserPhotoService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final Path uploadDirectory = Paths.get("uploads", "users").toAbsolutePath().normalize();

    public String storeUserPhoto(Long userId, MultipartFile file) {
        validateFile(file);

        try {
            Files.createDirectories(uploadDirectory);

            String extension = getExtension(file.getOriginalFilename());
            String filename = "user-" + userId + "-" + UUID.randomUUID() + "." + extension;
            Path target = uploadDirectory.resolve(filename).normalize();

            if (!target.startsWith(uploadDirectory)) {
                throw new RuntimeException("Invalid upload path");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/users/" + filename;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store user photo", ex);
        }
    }

    public void deleteUserPhoto(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) {
            return;
        }
        String prefix = "/uploads/users/";
        if (!photoUrl.startsWith(prefix)) {
            return;
        }

        try {
            Path target = uploadDirectory.resolve(photoUrl.substring(prefix.length())).normalize();
            if (target.startsWith(uploadDirectory)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not delete user photo", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Photo file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Photo file must not exceed 5MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Only jpg, jpeg, png and webp images are allowed");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new RuntimeException("Uploaded file must be an image");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new RuntimeException("Photo file extension is required");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
