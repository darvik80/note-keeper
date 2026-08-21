package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.dto.TodoInput;
import xyz.crearts.note.keeper.exception.AccessDeniedException;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.AttachmentMapper;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Todo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock private TodoMapper todoMapper;
    @Mock private AttachmentMapper attachmentMapper;
    @Mock private NotificationService notificationService;
    @Mock private TagSyncService tagSyncService;

    private TodoService todoService;
    private final ResourceAccessService resourceAccess = new ResourceAccessService();

    @BeforeEach
    void setUp() {
        todoService = new TodoService(todoMapper, attachmentMapper, notificationService, tagSyncService, resourceAccess);
    }

    private Todo buildTodo(String id, String ownerId) {
        Todo todo = new Todo();
        todo.setId(id);
        todo.setTitle("Test Todo");
        todo.setDescription("Description");
        todo.setTags(new ArrayList<>());
        todo.setPriority("medium");
        todo.setOwnerId(ownerId);
        todo.setSharedWith("[]");
        todo.setCreatedAt(LocalDateTime.now());
        todo.setUpdatedAt(LocalDateTime.now());
        return todo;
    }

    @Test
    void findById_existingTodo_shouldReturnTodo() {
        Todo todo = buildTodo("todo-1", "owner-1");
        when(todoMapper.findById("todo-1")).thenReturn(todo);

        Todo result = todoService.findById("todo-1", "owner-1");

        assertNotNull(result);
        assertEquals("todo-1", result.getId());
    }

    @Test
    void findById_nonExistent_shouldThrowException() {
        when(todoMapper.findById("missing")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> todoService.findById("missing", "owner-1"));
    }

    @Test
    void findAll_shouldCallMapper() {
        when(todoMapper.findRecurringActive("owner-1")).thenReturn(Collections.emptyList());
        when(todoMapper.findAll(any(), any(), any(), any(), any(), any(), eq("owner-1")))
                .thenReturn(Collections.emptyList());

        List<Todo> result = todoService.findAll(null, null, null, null, null, null, "owner-1");

        assertNotNull(result);
        verify(todoMapper).findRecurringActive("owner-1");
        verify(todoMapper).findAll(null, null, null, null, null, null, "owner-1");
    }

    @Test
    void findAll_shouldDropDeletedWhenNotRequested() {
        Todo live = buildTodo("live", "owner-1");
        live.setDeleted(false);
        live.setFavorite(true);
        Todo trash = buildTodo("trash", "owner-1");
        trash.setDeleted(true);
        trash.setFavorite(true);
        when(todoMapper.findRecurringActive("owner-1")).thenReturn(Collections.emptyList());
        when(todoMapper.findAll(null, null, null, true, false, false, "owner-1"))
                .thenReturn(List.of(live, trash));

        List<Todo> result = todoService.findAll(null, null, null, true, false, false, "owner-1");

        assertEquals(1, result.size());
        assertEquals("live", result.get(0).getId());
    }

    @Test
    void create_shouldInsertTodoAndSyncTags() {
        TodoInput input = new TodoInput();
        input.setTitle("New Todo");
        input.setTags(List.of("work", "urgent"));

        Todo created = buildTodo("new-id", "owner-1");
        when(todoMapper.findById(anyString())).thenReturn(created);

        Todo result = todoService.create(input, "owner-1");

        assertNotNull(result);
        verify(todoMapper).insert(any(Todo.class));
        verify(tagSyncService).addTags(eq("owner-1"), anyList());
        verify(notificationService).notifyTodoCreated(anyString(), eq("owner-1"));
    }

    @Test
    void update_scheduleNone_shouldWriteNoneRepeat() {
        Todo existing = buildTodo("todo-1", "owner-1");
        Todo.Schedule daily = new Todo.Schedule();
        daily.setRepeat("daily");
        existing.setSchedule(daily);
        when(todoMapper.findById("todo-1")).thenReturn(existing);

        TodoInput input = new TodoInput();
        input.setTitle("Updated Todo");
        input.setSchedule(java.util.Map.of("repeat", "none"));

        todoService.update("todo-1", input, "owner-1");

        verify(todoMapper).update(argThat(todo ->
                todo.getSchedule() != null && "none".equals(todo.getSchedule().getRepeat())));
    }

    @Test
    void create_withSchedule_shouldSetSchedule() {
        TodoInput input = new TodoInput();
        input.setTitle("Scheduled Todo");
        input.setSchedule(java.util.Map.of("repeat", "daily"));

        Todo created = buildTodo("new-id", "owner-1");
        when(todoMapper.findById(anyString())).thenReturn(created);

        todoService.create(input, "owner-1");

        verify(todoMapper).insert(argThat(todo ->
                todo.getSchedule() != null && "daily".equals(todo.getSchedule().getRepeat())));
    }

    @Test
    void update_shouldUpdateTodoAndSyncTags() {
        Todo existing = buildTodo("todo-1", "owner-1");
        when(todoMapper.findById("todo-1")).thenReturn(existing);

        TodoInput input = new TodoInput();
        input.setTitle("Updated Todo");
        input.setTags(List.of("updated"));

        todoService.update("todo-1", input, "owner-1");

        verify(todoMapper).update(any(Todo.class));
        verify(tagSyncService).updateTags(eq("owner-1"), anyList(), anyList());
        verify(notificationService).notifyTodoUpdated("todo-1", "owner-1");
    }

    @Test
    void update_reminderChange_shouldClearNotifiedAt() {
        Todo existing = buildTodo("todo-1", "owner-1");
        existing.setReminder(LocalDateTime.of(2026, 7, 7, 15, 30));
        existing.setNotifiedAt(LocalDateTime.of(2026, 7, 7, 15, 31));
        when(todoMapper.findById("todo-1")).thenReturn(existing);

        TodoInput input = new TodoInput();
        input.setTitle("Updated Todo");
        input.setReminder("2026-07-25T15:30:00");

        todoService.update("todo-1", input, "owner-1");

        verify(todoMapper).update(argThat(todo ->
                todo.getNotifiedAt() == null &&
                todo.getReminder() != null &&
                todo.getReminder().getDayOfMonth() == 25));
    }

    @Test
    void update_sameReminder_shouldKeepNotifiedAt() {
        LocalDateTime reminder = LocalDateTime.of(2026, 7, 25, 15, 30);
        LocalDateTime notified = LocalDateTime.of(2026, 7, 24, 15, 31);
        Todo existing = buildTodo("todo-1", "owner-1");
        existing.setReminder(reminder);
        existing.setNotifiedAt(notified);
        when(todoMapper.findById("todo-1")).thenReturn(existing);

        TodoInput input = new TodoInput();
        input.setTitle("Updated Todo");
        input.setReminder("2026-07-25T15:30:00");

        todoService.update("todo-1", input, "owner-1");

        verify(todoMapper).update(argThat(todo -> notified.equals(todo.getNotifiedAt())));
    }

    @Test
    void delete_softDelete_shouldCallSoftDelete() {
        Todo todo = buildTodo("todo-1", "owner-1");
        when(todoMapper.findById("todo-1")).thenReturn(todo);

        todoService.delete("todo-1", false, "owner-1");

        verify(todoMapper).softDelete(eq("todo-1"), any(LocalDateTime.class));
        verify(todoMapper, never()).permanentDelete(any());
    }

    @Test
    void delete_permanentDelete_shouldCallPermanentDelete() {
        Todo todo = buildTodo("todo-1", "owner-1");
        when(todoMapper.findById("todo-1")).thenReturn(todo);

        todoService.delete("todo-1", true, "owner-1");

        verify(todoMapper).permanentDelete("todo-1");
        verify(attachmentMapper).deleteByParent("todo-1", "todo");
    }

    @Test
    void delete_nonExistent_shouldThrowException() {
        when(todoMapper.findById("missing")).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> todoService.delete("missing", false, "owner-1"));
    }

    @Test
    void archive_shouldCallMapperArchive() {
        Todo todo = buildTodo("todo-1", "owner-1");
        when(todoMapper.findById("todo-1")).thenReturn(todo);

        todoService.archive("todo-1", "owner-1");

        verify(todoMapper).archive("todo-1");
    }

    @Test
    void restore_shouldCallMapperRestore() {
        Todo todo = buildTodo("todo-1", "owner-1");
        when(todoMapper.findById("todo-1")).thenReturn(todo);

        todoService.restore("todo-1", "owner-1");

        verify(todoMapper).restore("todo-1");
    }

    @Test
    void shareWithUser_ownerCanShare() {
        Todo todo = buildTodo("todo-1", "owner-1");
        when(todoMapper.findById("todo-1")).thenReturn(todo);

        todoService.shareWithUser("todo-1", "user-2", "owner-1");

        verify(todoMapper).shareWithUser(eq("todo-1"), anyString());
    }

    @Test
    void shareWithUser_nonOwner_shouldThrowException() {
        Todo todo = buildTodo("todo-1", "owner-1");
        when(todoMapper.findById("todo-1")).thenReturn(todo);

        assertThrows(AccessDeniedException.class,
                () -> todoService.shareWithUser("todo-1", "user-2", "not-owner"));
    }

    @Test
    void unshareWithUser_ownerCanUnshare() {
        Todo todo = buildTodo("todo-1", "owner-1");
        todo.setSharedWith("[\"user-2\"]");
        when(todoMapper.findById("todo-1")).thenReturn(todo);

        todoService.unshareWithUser("todo-1", "user-2", "owner-1");

        verify(todoMapper).shareWithUser(eq("todo-1"), anyString());
    }

    @Test
    void findSharedWithMe_shouldCallMapper() {
        when(todoMapper.findSharedWithMe("user-1")).thenReturn(Collections.emptyList());

        List<Todo> result = todoService.findSharedWithMe("user-1");

        assertNotNull(result);
        verify(todoMapper).findSharedWithMe("user-1");
    }

    private Todo dailyTodo(String id, String ownerId, LocalDateTime reminder) {
        Todo todo = buildTodo(id, ownerId);
        todo.setReminder(reminder);
        todo.setCompleted(false);
        Todo.Schedule schedule = new Todo.Schedule();
        schedule.setRepeat("daily");
        todo.setSchedule(schedule);
        return todo;
    }

    @Test
    void toggleComplete_recurring_logsAndSetsNextReminder() {
        LocalDateTime now = Recurrence.nowUtc();
        LocalDateTime reminder = now.withHour(15).withMinute(30).withSecond(0).withNano(0);
        Todo todo = dailyTodo("todo-1", "owner-1", reminder);
        when(todoMapper.findById("todo-1")).thenReturn(todo);

        Todo result = todoService.toggleComplete("todo-1", "owner-1");

        assertTrue(result.isCompleted());
        assertEquals(reminder.toLocalDate().plusDays(1), result.getReminder().toLocalDate());
        verify(todoMapper).insertCompletionLog(any());
        verify(notificationService).notifyTodoUpdated("todo-1", "owner-1");
    }

    @Test
    void toggleComplete_recurringAlreadyDoneToday_doesNotSimpleToggle() {
        LocalDateTime now = Recurrence.nowUtc();
        LocalDateTime slot = now.withHour(15).withMinute(30).withSecond(0).withNano(0);
        Todo todo = dailyTodo("todo-1", "owner-1", slot.plusDays(1));
        todo.setCompleted(true);
        todo.setLastCompletedAt(now.toLocalDate().atTime(3, 10));
        when(todoMapper.findById("todo-1")).thenReturn(todo);
        when(todoMapper.findCompletionLog("todo-1")).thenReturn(Collections.emptyList());

        Todo result = todoService.toggleComplete("todo-1", "owner-1");

        assertFalse(result.isCompleted());
        verify(todoMapper, never()).insertCompletionLog(any());
        verify(todoMapper).deleteCompletionLogsSince(eq("todo-1"), any());
    }

    @Test
    void findAll_rolloversRecurringBeforeQuery() {
        when(todoMapper.findRecurringActive("owner-1")).thenReturn(Collections.emptyList());
        when(todoMapper.findAll(null, null, null, null, false, false, "owner-1"))
                .thenReturn(Collections.emptyList());

        todoService.findAll(null, null, null, null, false, false, "owner-1");

        verify(todoMapper).findRecurringActive("owner-1");
        verify(todoMapper).findAll(null, null, null, null, false, false, "owner-1");
    }
}
