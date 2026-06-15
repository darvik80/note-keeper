package xyz.crearts.note.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Publishes real-time CRUD events over WebSocket to /topic/updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyTodoCreated(String todoId, String ownerId) {
        sendEvent("TODO_CREATED", Map.of("id", todoId, "ownerId", ownerId));
    }

    public void notifyTodoUpdated(String todoId, String ownerId) {
        sendEvent("TODO_UPDATED", Map.of("id", todoId, "ownerId", ownerId));
    }

    public void notifyTodoDeleted(String todoId, String ownerId) {
        sendEvent("TODO_DELETED", Map.of("id", todoId, "ownerId", ownerId));
    }

    public void notifyNoteCreated(String noteId, String ownerId) {
        sendEvent("NOTE_CREATED", Map.of("id", noteId, "ownerId", ownerId));
    }

    public void notifyNoteUpdated(String noteId, String ownerId) {
        sendEvent("NOTE_UPDATED", Map.of("id", noteId, "ownerId", ownerId));
    }

    public void notifyNoteDeleted(String noteId, String ownerId) {
        sendEvent("NOTE_DELETED", Map.of("id", noteId, "ownerId", ownerId));
    }

    private void sendEvent(String type, Map<String, String> payload) {
        try {
            Map<String, Object> message = Map.of("type", type, "data", payload);
            messagingTemplate.convertAndSend("/topic/updates", message);
            log.debug("WS event sent: {}", type);
        } catch (Exception e) {
            log.warn("Failed to send WS event {}: {}", type, e.getMessage());
        }
    }
}
