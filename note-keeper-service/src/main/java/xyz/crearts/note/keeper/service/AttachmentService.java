package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.AttachmentMapper;
import xyz.crearts.note.keeper.mapper.NoteMapper;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Attachment;
import xyz.crearts.note.keeper.model.Note;
import xyz.crearts.note.keeper.model.Todo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
public class AttachmentService {

    private final AttachmentMapper attachmentMapper;
    private final NoteMapper noteMapper;
    private final TodoMapper todoMapper;
    private final ResourceAccessService resourceAccess;
    private final String baseDir;

    public AttachmentService(
            AttachmentMapper attachmentMapper,
            NoteMapper noteMapper,
            TodoMapper todoMapper,
            ResourceAccessService resourceAccess,
            @Value("${app.storage.attachments-dir}") String attachmentsDir) {
        this.attachmentMapper = attachmentMapper;
        this.noteMapper = noteMapper;
        this.todoMapper = todoMapper;
        this.resourceAccess = resourceAccess;
        this.baseDir = attachmentsDir;
        try {
            Files.createDirectories(Paths.get(baseDir));
            log.info("Attachments base directory: {}", baseDir);
        } catch (IOException e) {
            log.error("Failed to create attachments directory", e);
        }
    }

    public Attachment uploadFile(MultipartFile file, String parentId, String parentType, String userId) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!"note".equals(parentType) && !"todo".equals(parentType)) {
            throw new IllegalArgumentException("Invalid parentType: " + parentType);
        }

        String parentOwnerId = requireParentWriteAccess(parentId, parentType, userId);

        String dirPath = baseDir + "/" + parentOwnerId + "/" + parentId;
        Files.createDirectories(Paths.get(dirPath));

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        Attachment attachment = new Attachment();
        attachment.setId(UUID.randomUUID().toString());
        String storedFilename = attachment.getId() + extension;

        Path filePath = Paths.get(dirPath, storedFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        attachment.setParentId(parentId);
        attachment.setParentType(parentType);
        attachment.setName(originalFilename != null ? originalFilename : storedFilename);
        attachment.setSize(file.getSize());
        attachment.setType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        attachment.setUrl("/api/v1/attachments/" + attachment.getId() + "/download");
        attachment.setUploadedAt(LocalDateTime.now());

        attachmentMapper.insert(attachment);

        log.info("File uploaded: {} -> {}", originalFilename, filePath);
        return attachment;
    }

    public List<Attachment> uploadFiles(MultipartFile[] files, String parentId, String parentType, String userId) throws IOException {
        List<Attachment> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                attachments.add(uploadFile(file, parentId, parentType, userId));
            }
        }
        return attachments;
    }

    public void deleteAttachment(String attachmentId, String userId) {
        Attachment attachment = attachmentMapper.findById(attachmentId);
        if (attachment == null) {
            throw new ResourceNotFoundException("Attachment not found: " + attachmentId);
        }

        requireAttachmentWriteAccess(attachment, userId);

        String parentOwnerId = resolveParentOwnerId(attachment);
        try {
            Path filePath = resolveAttachmentPath(parentOwnerId, attachment);
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete attachment file for {}", attachmentId, e);
        }

        attachmentMapper.deleteById(attachmentId);
        log.info("Deleted attachment: {}", attachmentId);
    }

    public byte[] getFileContent(String attachmentId, String userId) throws IOException {
        Attachment attachment = attachmentMapper.findById(attachmentId);
        if (attachment == null) {
            throw new ResourceNotFoundException("Attachment not found: " + attachmentId);
        }

        requireAttachmentReadAccess(attachment, userId);

        String parentOwnerId = resolveParentOwnerId(attachment);
        Path filePath = resolveAttachmentPath(parentOwnerId, attachment);
        return Files.readAllBytes(filePath);
    }

    public Attachment getAttachment(String attachmentId) {
        return attachmentMapper.findById(attachmentId);
    }

    private String requireParentWriteAccess(String parentId, String parentType, String userId) {
        if ("note".equals(parentType)) {
            Note note = noteMapper.findById(parentId);
            if (note == null) {
                throw new ResourceNotFoundException("Note not found: " + parentId);
            }
            resourceAccess.requireNoteOwner(note, userId);
            return note.getOwnerId();
        }

        Todo todo = todoMapper.findById(parentId);
        if (todo == null) {
            throw new ResourceNotFoundException("Todo not found: " + parentId);
        }
        resourceAccess.requireTodoOwner(todo, userId);
        return todo.getOwnerId();
    }

    private void requireAttachmentReadAccess(Attachment attachment, String userId) {
        if ("note".equals(attachment.getParentType())) {
            Note note = noteMapper.findById(attachment.getParentId());
            if (note == null) {
                throw new ResourceNotFoundException("Note not found: " + attachment.getParentId());
            }
            resourceAccess.requireNoteRead(note, userId);
            return;
        }

        Todo todo = todoMapper.findById(attachment.getParentId());
        if (todo == null) {
            throw new ResourceNotFoundException("Todo not found: " + attachment.getParentId());
        }
        resourceAccess.requireTodoRead(todo, userId);
    }

    private void requireAttachmentWriteAccess(Attachment attachment, String userId) {
        if ("note".equals(attachment.getParentType())) {
            Note note = noteMapper.findById(attachment.getParentId());
            if (note == null) {
                throw new ResourceNotFoundException("Note not found: " + attachment.getParentId());
            }
            resourceAccess.requireNoteOwner(note, userId);
            return;
        }

        Todo todo = todoMapper.findById(attachment.getParentId());
        if (todo == null) {
            throw new ResourceNotFoundException("Todo not found: " + attachment.getParentId());
        }
        resourceAccess.requireTodoOwner(todo, userId);
    }

    private String resolveParentOwnerId(Attachment attachment) {
        if ("note".equals(attachment.getParentType())) {
            Note note = noteMapper.findById(attachment.getParentId());
            if (note == null) {
                throw new ResourceNotFoundException("Note not found: " + attachment.getParentId());
            }
            return note.getOwnerId();
        }

        Todo todo = todoMapper.findById(attachment.getParentId());
        if (todo == null) {
            throw new ResourceNotFoundException("Todo not found: " + attachment.getParentId());
        }
        return todo.getOwnerId();
    }

    private Path resolveAttachmentPath(String parentOwnerId, Attachment attachment) throws IOException {
        Path dir = Paths.get(baseDir, parentOwnerId, attachment.getParentId());
        if (!Files.exists(dir)) {
            throw new ResourceNotFoundException("Attachment file not found");
        }

        try (Stream<Path> files = Files.list(dir)) {
            return files
                .filter(path -> path.getFileName().toString().startsWith(attachment.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Attachment file not found"));
        }
    }
}
