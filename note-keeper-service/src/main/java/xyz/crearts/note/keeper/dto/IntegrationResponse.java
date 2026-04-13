package xyz.crearts.note.keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationResponse {
    private boolean success;
    private String message;
}
