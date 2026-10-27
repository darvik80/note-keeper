package dev.cogitor.note.keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import dev.cogitor.note.keeper.model.VaultFolder;
import dev.cogitor.note.keeper.model.VaultItem;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VaultSyncResponse {
    private List<VaultFolder> folders;
    private List<VaultItem> items;
    private List<String> deletedIds;
    private LocalDateTime serverTime;
}
