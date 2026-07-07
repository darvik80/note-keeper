package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Captor private ArgumentCaptor<String> destinationCaptor;
    @Captor private ArgumentCaptor<Object> messageCaptor;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(messagingTemplate);
    }

    private Map<String, Object> captureMessage(String methodName) {
        verify(messagingTemplate).convertAndSend(destinationCaptor.capture(), messageCaptor.capture());
        assertEquals("/topic/updates", destinationCaptor.getValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) messageCaptor.getValue();
        return message;
    }

    @Test
    void notifyTodoCreated_shouldSendEvent() {
        notificationService.notifyTodoCreated("todo-1", "owner-1");

        Map<String, Object> message = captureMessage("notifyTodoCreated");
        assertEquals("TODO_CREATED", message.get("type"));
    }

    @Test
    void notifyTodoUpdated_shouldSendEvent() {
        notificationService.notifyTodoUpdated("todo-1", "owner-1");

        Map<String, Object> message = captureMessage("notifyTodoUpdated");
        assertEquals("TODO_UPDATED", message.get("type"));
    }

    @Test
    void notifyTodoDeleted_shouldSendEvent() {
        notificationService.notifyTodoDeleted("todo-1", "owner-1");

        Map<String, Object> message = captureMessage("notifyTodoDeleted");
        assertEquals("TODO_DELETED", message.get("type"));
    }

    @Test
    void notifyNoteCreated_shouldSendEvent() {
        notificationService.notifyNoteCreated("note-1", "owner-1");

        Map<String, Object> message = captureMessage("notifyNoteCreated");
        assertEquals("NOTE_CREATED", message.get("type"));
    }

    @Test
    void notifyNoteUpdated_shouldSendEvent() {
        notificationService.notifyNoteUpdated("note-1", "owner-1");

        Map<String, Object> message = captureMessage("notifyNoteUpdated");
        assertEquals("NOTE_UPDATED", message.get("type"));
    }

    @Test
    void notifyNoteDeleted_shouldSendEvent() {
        notificationService.notifyNoteDeleted("note-1", "owner-1");

        Map<String, Object> message = captureMessage("notifyNoteDeleted");
        assertEquals("NOTE_DELETED", message.get("type"));
    }

    @Test
    void sendEvent_messagingTemplateFails_shouldNotThrow() {
        doThrow(new RuntimeException("WS error"))
                .when(messagingTemplate).convertAndSend(any(String.class), any(Object.class));

        // Should not throw - exception is caught internally
        assertDoesNotThrow(() -> notificationService.notifyNoteCreated("note-1", "owner-1"));
    }

    @Test
    void sendEvent_shouldContainOwnerIdInPayload() {
        notificationService.notifyNoteCreated("note-1", "owner-1");

        Map<String, Object> message = captureMessage("notifyNoteCreated");
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) message.get("data");
        assertEquals("owner-1", data.get("ownerId"));
        assertEquals("note-1", data.get("id"));
    }
}
