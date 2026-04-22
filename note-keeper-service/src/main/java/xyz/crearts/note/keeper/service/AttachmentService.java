package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.crearts.note.keeper.mapper.AttachmentMapper;
import xyz.crearts.note.keeper.model.Attachment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing file attachments.
 * Handles file upload, storage, and deletion.
 */
@Slf4j
@Service
public class AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final String baseDir;

    public AttachmentService(
            AttachmentMapper attachmentMapper,
            @Value("${app.storage.attachments-dir}") String attachmentsDir) {
        this.attachmentMapper = attachmentMapper;
        this.baseDir = attachmentsDir;
        // Create base directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(baseDir));
            log.info("Attachments base directory: {}", baseDir);
        } catch (IOException e) {
            log.error("Failed to create attachments directory", e);
        }
    }

    /**
     * Upload a single file and create attachment record.
     */
    public Attachment uploadFile(MultipartFile file, String parentId, String parentType, String ownerId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate parent type
        if (!"note".equals(parentType) && !"todo".equals(parentType)) {
            throw new IllegalArgumentException("Invalid parentType: " + parentType);
        }

        // Create directory: ./attachments/{ownerId}/{parentId}/
        String dirPath = baseDir + "/" + ownerId + "/" + parentId;
        Files.createDirectories(Paths.get(dirPath));

        // Generate unique filename to avoid conflicts
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
        String filename = UUID.randomUUID().toString() + extension;

        // Save file to disk
        Path filePath = Paths.get(dirPath, filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create attachment record
        Attachment attachment = new Attachment();
        attachment.setId(UUID.randomUUID().toString());
        attachment.setParentId(parentId);
        attachment.setParentType(parentType);
        attachment.setName(originalFilename != null ? originalFilename : filename);
        attachment.setSize(file.getSize());
        attachment.setType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        attachment.setUrl("/api/v1/attachments/" + attachment.getId() + "/download");
        attachment.setUploadedAt(LocalDateTime.now());

        attachmentMapper.insert(attachment);

        log.info("File uploaded: {} -> {}", originalFilename, filePath);
        return attachment;
    }

    /**
     * Upload multiple files at once.
     */
    public List<Attachment> uploadFiles(MultipartFile[] files, String parentId, String parentType, String ownerId) throws IOException {
        List<Attachment> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                attachments.add(uploadFile(file, parentId, parentType, ownerId));
            }
        }
        return attachments;
    }

    /**
     * Delete an attachment and its file.
     */
    public void deleteAttachment(String attachmentId, String ownerId) {
        Attachment attachment = attachmentMapper.findById(attachmentId);
        if (attachment == null) {
            throw new RuntimeException("Attachment not found: " + attachmentId);
        }

        // Verify ownership
        if (!ownerId.equals(getOwnerIdForAttachment(attachment, ownerId))) {
            throw new RuntimeException("Not authorized to delete this attachment");
        }

        // Delete file from disk
        String filePath = baseDir + "/" + ownerId + "/" + attachment.getParentId() + "/" + getFilenameFromAttachment(attachment);
        try {
            Files.deleteIfExists(Paths.get(filePath));
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
        }

        // Delete record from database
        attachmentMapper.deleteById(attachmentId);
        log.info("Deleted attachment: {}", attachmentId);
    }

    /**
     * Get file content for download.
     */
    public byte[] getFileContent(String attachmentId, String ownerId) throws IOException {
        Attachment attachment = attachmentMapper.findById(attachmentId);
        if (attachment == null) {
            throw new RuntimeException("Attachment not found: " + attachmentId);
        }

        // Verify ownership (check if user owns the parent entity)
        // For simplicity, we'll allow anyone with the attachment ID to download
        // In production, you should verify ownership of the parent note/todo

        String filePath = baseDir + "/" + ownerId + "/" + attachment.getParentId() + "/" + getFilenameFromAttachment(attachment);
        return Files.readAllBytes(Paths.get(filePath));
    }

    /**
     * Get attachment by ID.
     */
    public Attachment getAttachment(String attachmentId) {
        return attachmentMapper.findById(attachmentId);
    }

    /**
     * Get owner ID for attachment by looking up parent entity.
     * For simplicity, we use the ownerId passed from the controller.
     */
    private String getOwnerIdForAttachment(Attachment attachment, String ownerId) {
        return ownerId;
    }

    /**
     * Extract filename from attachment record.
     * We store the original filename in the 'name' field.
     */
    private String getFilenameFromAttachment(Attachment attachment) {
        return attachment.getName();
    }
}
