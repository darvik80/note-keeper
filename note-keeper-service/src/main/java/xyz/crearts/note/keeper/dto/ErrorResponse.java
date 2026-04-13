package xyz.crearts.note.keeper.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private Map<String, Object> details;

    public ErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }
}
