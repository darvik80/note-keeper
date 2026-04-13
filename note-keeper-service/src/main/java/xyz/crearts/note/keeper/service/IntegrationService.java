package xyz.crearts.note.keeper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.dto.IntegrationRequest;
import xyz.crearts.note.keeper.dto.IntegrationResponse;

@Service
public class IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationService.class);

    public IntegrationResponse sendToTelegram(IntegrationRequest request) {
        log.info("Telegram integration called with message: {}", request.getMessage());
        // Stub implementation - integrate with Telegram Bot API in future
        return new IntegrationResponse(true, "Message sent to Telegram (stub)");
    }

    public IntegrationResponse sendToDingTalk(IntegrationRequest request) {
        log.info("DingTalk integration called with message: {}", request.getMessage());
        // Stub implementation - integrate with DingTalk webhook in future
        return new IntegrationResponse(true, "Message sent to DingTalk (stub)");
    }

    public IntegrationResponse sendEmail(IntegrationRequest request) {
        log.info("Email integration called with subject: {}, message: {}", request.getSubject(), request.getMessage());
        // Stub implementation - integrate with JavaMailSender in future
        return new IntegrationResponse(true, "Email sent successfully (stub)");
    }
}
