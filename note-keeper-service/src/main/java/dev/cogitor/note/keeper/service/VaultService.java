package dev.cogitor.note.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import dev.cogitor.note.keeper.dto.VaultFolderInput;
import dev.cogitor.note.keeper.dto.VaultItemInput;
import dev.cogitor.note.keeper.dto.VaultSyncResponse;
import dev.cogitor.note.keeper.exception.ConflictException;
import dev.cogitor.note.keeper.exception.ResourceNotFoundException;
import dev.cogitor.note.keeper.mapper.UserSettingsMapper;
import dev.cogitor.note.keeper.mapper.VaultFolderMapper;
import dev.cogitor.note.keeper.mapper.VaultItemMapper;
import dev.cogitor.note.keeper.model.VaultFolder;
import dev.cogitor.note.keeper.model.VaultItem;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaultService {

    private static final Set<String> TYPES = Set.of("password", "ssh_key", "api_token", "note");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final VaultFolderMapper folderMapper;
    private final VaultItemMapper itemMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final EncryptionService encryptionService;

    public List<VaultFolder> listFolders(String ownerId) {
        return folderMapper.findByOwner(ownerId);
    }

    public VaultFolder createFolder(String ownerId, VaultFolderInput input) {
        VaultFolder folder = new VaultFolder();
        folder.setId(UUID.randomUUID().toString());
        folder.setOwnerId(ownerId);
        folder.setParentId(blankToNull(input.getParentId()));
        folder.setName(input.getName().trim());
        folder.setUpdatedAt(LocalDateTime.now());
        folderMapper.insert(folder);
        return folder;
    }

    public VaultFolder updateFolder(String ownerId, String id, VaultFolderInput input) {
        VaultFolder existing = requireFolder(id, ownerId);
        existing.setName(input.getName().trim());
        existing.setParentId(blankToNull(input.getParentId()));
        existing.setUpdatedAt(LocalDateTime.now());
        folderMapper.update(existing);
        return existing;
    }

    public void deleteFolder(String ownerId, String id) {
        requireFolder(id, ownerId);
        folderMapper.delete(id, ownerId);
    }

    public List<VaultItem> listItems(String ownerId, String folderId, String q, LocalDateTime since, boolean includeDeleted) {
        List<VaultItem> items = itemMapper.findByOwner(ownerId, folderId, q, since, includeDeleted);
        byte[] key = userVaultKey(ownerId);
        items.forEach(item -> decryptSecrets(item, key));
        return items;
    }

    public VaultItem getItem(String ownerId, String id) {
        VaultItem item = itemMapper.findById(id, ownerId);
        if (item == null || item.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Vault item not found");
        }
        decryptSecrets(item, userVaultKey(ownerId));
        return item;
    }

    public VaultItem createItem(String ownerId, VaultItemInput input) {
        if (input.getId() != null && !input.getId().isBlank()) {
            VaultItem existing = itemMapper.findById(input.getId(), ownerId);
            if (existing != null && existing.getDeletedAt() == null) {
                return updateItem(ownerId, existing.getId(), input, null);
            }
        }
        VaultItem item = new VaultItem();
        item.setId(input.getId() != null && !input.getId().isBlank() ? input.getId() : UUID.randomUUID().toString());
        item.setOwnerId(ownerId);
        applyInput(item, input);
        item.setRevision(1);
        item.setUpdatedAt(LocalDateTime.now());
        item.setSecretBlob(encryptSecrets(input, userVaultKey(ownerId)));
        itemMapper.insert(item);
        decryptSecrets(item, userVaultKey(ownerId));
        return item;
    }

    public VaultItem updateItem(String ownerId, String id, VaultItemInput input, Integer expectedRevision) {
        VaultItem existing = itemMapper.findById(id, ownerId);
        if (existing == null || existing.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Vault item not found");
        }
        if (expectedRevision != null && !expectedRevision.equals(existing.getRevision())) {
            throw new ConflictException("Revision mismatch: expected " + expectedRevision + ", was " + existing.getRevision());
        }
        applyInput(existing, input);
        existing.setRevision(existing.getRevision() + 1);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setSecretBlob(encryptSecrets(input, userVaultKey(ownerId)));
        itemMapper.update(existing);
        decryptSecrets(existing, userVaultKey(ownerId));
        return existing;
    }

    public void deleteItem(String ownerId, String id) {
        VaultItem existing = itemMapper.findById(id, ownerId);
        if (existing == null || existing.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Vault item not found");
        }
        LocalDateTime now = LocalDateTime.now();
        itemMapper.tombstone(id, ownerId, existing.getRevision() + 1, now);
    }

    public VaultSyncResponse sync(String ownerId, LocalDateTime since) {
        List<VaultFolder> folders = folderMapper.findChangedSince(ownerId, since);
        List<VaultItem> changed = itemMapper.findChangedSince(ownerId, since);
        byte[] key = userVaultKey(ownerId);
        List<VaultItem> items = new ArrayList<>();
        List<String> deletedIds = new ArrayList<>();
        for (VaultItem item : changed) {
            if (item.getDeletedAt() != null) {
                deletedIds.add(item.getId());
            } else {
                decryptSecrets(item, key);
                items.add(item);
            }
        }
        return new VaultSyncResponse(folders, items, deletedIds, LocalDateTime.now());
    }

    private void applyInput(VaultItem item, VaultItemInput input) {
        String type = input.getType() == null || input.getType().isBlank() ? "password" : input.getType();
        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid vault item type: " + type);
        }
        item.setType(type);
        item.setTitle(input.getTitle().trim());
        item.setFolderId(blankToNull(input.getFolderId()));
        item.setUsername(input.getUsername());
        item.setHost(input.getHost());
        item.setUrl(input.getUrl());
        item.setTags(input.getTags() != null ? input.getTags() : new ArrayList<>());
    }

    private String encryptSecrets(VaultItemInput input, byte[] key) {
        Map<String, String> blob = new LinkedHashMap<>();
        putIfPresent(blob, "password", input.getPassword());
        putIfPresent(blob, "privateKey", input.getPrivateKey());
        putIfPresent(blob, "secret", input.getSecret());
        putIfPresent(blob, "extraJson", input.getExtraJson());
        if (blob.isEmpty()) {
            return null;
        }
        return encryptionService.encryptWithKey(JSON.writeValueAsString(blob), key);
    }

    private void decryptSecrets(VaultItem item, byte[] key) {
        if (item.getSecretBlob() == null || item.getSecretBlob().isBlank()) {
            return;
        }
        try {
            String json = encryptionService.decryptWithKey(item.getSecretBlob(), key);
            Map<?, ?> blob = JSON.readValue(json, Map.class);
            item.setPassword(asString(blob.get("password")));
            item.setPrivateKey(asString(blob.get("privateKey")));
            item.setSecret(asString(blob.get("secret")));
            item.setExtraJson(asString(blob.get("extraJson")));
        } catch (Exception e) {
            log.warn("Failed to decrypt vault item {}", item.getId());
        }
        item.setSecretBlob(null);
    }

    private byte[] userVaultKey(String ownerId) {
        String wrapped = userSettingsMapper.findVaultKey(ownerId);
        if (wrapped == null || wrapped.isBlank()) {
            byte[] raw = EncryptionService.randomKeyBytes();
            String encoded = java.util.Base64.getEncoder().encodeToString(raw);
            wrapped = encryptionService.encrypt(encoded);
            userSettingsMapper.upsertVaultKey(ownerId, wrapped, LocalDateTime.now());
            return raw;
        }
        String encoded = encryptionService.decrypt(wrapped);
        return java.util.Base64.getDecoder().decode(encoded);
    }

    private VaultFolder requireFolder(String id, String ownerId) {
        VaultFolder folder = folderMapper.findById(id, ownerId);
        if (folder == null) {
            throw new ResourceNotFoundException("Vault folder not found");
        }
        return folder;
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
