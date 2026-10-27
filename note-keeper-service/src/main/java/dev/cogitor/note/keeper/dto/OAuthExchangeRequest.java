package dev.cogitor.note.keeper.dto;

import lombok.Data;

@Data
public class OAuthExchangeRequest {
    private String code;
    private String state;
}
