package xyz.crearts.note.keeper.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xyz.crearts.note.keeper.model.Attachment;
import xyz.crearts.note.keeper.service.AttachmentService;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing file attachments.
 * Handles file upload, download, and deletion.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * Upload a file and create attachment record.
     * @param file The file to upload
     * @param parentId The parent entity ID (note or todo ID)
     * @param parentType The parent entity type ("note" or "todo")
     * @param ownerId The owner user ID (from JWT)
     * @return The created attachment metadata
     */
    @PostMapping("/upload")
    public ResponseEntity<Attachment> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("parentId") String parentId,
            @RequestParam("parentType") String parentType,
            @AuthenticationPrincipal String ownerId) {
        
        log.info("Uploading file: {} for {} {} by user {}", 
                file.getOriginalFilename(), parentType, parentId, ownerId);
        
        try {
            Attachment attachment = attachmentService.uploadFile(file, parentId, parentType, ownerId);
            return ResponseEntity.ok(attachment);
        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Upload multiple files at once.
     * @param files The files to upload
     * @param parentId The parent entity ID
     * @param parentType The parent entity type
     * @param ownerId The owner user ID
     * @return List of created attachment metadata
     */
    @PostMapping("/upload-batch")
    public ResponseEntity<List<Attachment>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("parentId") String parentId,
            @RequestParam("parentType") String parentType,
            @AuthenticationPrincipal String ownerId) {
        
        log.info("Uploading {} files for {} {} by user {}", 
                files.length, parentType, parentId, ownerId);
        
        try {
            List<Attachment> attachments = attachmentService.uploadFiles(files, parentId, parentType, ownerId);
            return ResponseEntity.ok(attachments);
        } catch (Exception e) {
            log.error("Batch file upload failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete an attachment and its file.
     * @param attachmentId The attachment ID
     * @param ownerId The owner user ID
     * @return No content
     */
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable String attachmentId,
            @AuthenticationPrincipal String ownerId) {
        
        log.info("Deleting attachment {} by user {}", attachmentId, ownerId);
        
        try {
            attachmentService.deleteAttachment(attachmentId, ownerId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Attachment deletion failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get file content for download.
     * @param attachmentId The attachment ID
     * @param ownerId The owner user ID
     * @return File content as byte array
     */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable String attachmentId,
            @AuthenticationPrincipal String ownerId) {
        
        try {
            byte[] fileContent = attachmentService.getFileContent(attachmentId, ownerId);
            Attachment attachment = attachmentService.getAttachment(attachmentId);
            
            return ResponseEntity.ok()
                    .header("Content-Type", attachment.getType())
                    .header("Content-Disposition", 
                            "attachment; filename=\"" + attachment.getName() + "\"")
                    .body(fileContent);
        } catch (Exception e) {
            log.error("File download failed", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
