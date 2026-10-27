package dev.cogitor.note.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import dev.cogitor.note.keeper.dto.NoteInput;
import dev.cogitor.note.keeper.dto.SearchResult;
import dev.cogitor.note.keeper.dto.TodoInput;
import dev.cogitor.note.keeper.model.Note;
import dev.cogitor.note.keeper.model.Todo;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpService {

    private static final String PROTOCOL = "2025-03-26";
    private final JsonMapper json = JsonMapper.builder().build();
    private final NoteService noteService;
    private final TodoService todoService;
    private final SearchService searchService;

    public ObjectNode handle(JsonNode request, String ownerId) {
        ObjectNode response = json.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (request != null && request.get("id") != null && !request.get("id").isNull()) {
            response.set("id", request.get("id"));
        }
        String method = request == null || request.get("method") == null ? "" : request.get("method").asString("");
        JsonNode params = request == null ? null : request.get("params");
        try {
            response.set("result", dispatch(method, params, ownerId));
        } catch (IllegalArgumentException e) {
            ObjectNode error = json.createObjectNode();
            error.put("code", -32601);
            error.put("message", e.getMessage());
            response.set("error", error);
            response.remove("result");
        } catch (Exception e) {
            log.warn("MCP method {} failed: {}", method, e.getMessage());
            ObjectNode error = json.createObjectNode();
            error.put("code", -32000);
            error.put("message", e.getMessage() == null ? "Internal error" : e.getMessage());
            response.set("error", error);
            response.remove("result");
        }
        return response;
    }

    private JsonNode dispatch(String method, JsonNode params, String ownerId) {
        return switch (method) {
            case "initialize" -> initialize();
            case "notifications/initialized", "ping" -> json.createObjectNode();
            case "tools/list" -> toolsList();
            case "tools/call" -> toolsCall(params, ownerId);
            case "resources/list" -> resourcesList(ownerId);
            case "resources/read" -> resourcesRead(params, ownerId);
            default -> throw new IllegalArgumentException("Unknown method: " + method);
        };
    }

    private ObjectNode initialize() {
        ObjectNode result = json.createObjectNode();
        result.put("protocolVersion", PROTOCOL);
        ObjectNode caps = json.createObjectNode();
        caps.set("tools", json.createObjectNode());
        caps.set("resources", json.createObjectNode());
        result.set("capabilities", caps);
        ObjectNode info = json.createObjectNode();
        info.put("name", "notekeeper");
        info.put("version", "2.0.0");
        result.set("serverInfo", info);
        return result;
    }

    private ObjectNode toolsList() {
        ArrayNode tools = json.createArrayNode();
        tools.add(tool("search", "Search notes and todos", Map.of(
                "query", Map.of("type", "string"),
                "type", Map.of("type", "string", "description", "all|notes|todos")
        ), List.of("query")));
        tools.add(tool("list_notes", "List notes", Map.of(
                "folder", Map.of("type", "string"),
                "tag", Map.of("type", "string")
        ), List.of()));
        tools.add(tool("get_note", "Get a note by id", Map.of(
                "id", Map.of("type", "string")
        ), List.of("id")));
        tools.add(tool("create_note", "Create a note", Map.of(
                "title", Map.of("type", "string"),
                "content", Map.of("type", "string"),
                "folder", Map.of("type", "string"),
                "tags", Map.of("type", "array", "items", Map.of("type", "string"))
        ), List.of("title")));
        tools.add(tool("update_note", "Update a note", Map.of(
                "id", Map.of("type", "string"),
                "title", Map.of("type", "string"),
                "content", Map.of("type", "string")
        ), List.of("id", "title")));
        tools.add(tool("list_todos", "List todos", Map.of(
                "completed", Map.of("type", "boolean"),
                "tag", Map.of("type", "string")
        ), List.of()));
        tools.add(tool("get_todo", "Get a todo by id", Map.of(
                "id", Map.of("type", "string")
        ), List.of("id")));
        tools.add(tool("create_todo", "Create a todo", Map.of(
                "title", Map.of("type", "string"),
                "description", Map.of("type", "string")
        ), List.of("title")));
        tools.add(tool("toggle_todo", "Toggle todo completed state", Map.of(
                "id", Map.of("type", "string")
        ), List.of("id")));
        ObjectNode result = json.createObjectNode();
        result.set("tools", tools);
        return result;
    }

    private ObjectNode tool(String name, String description, Map<String, Object> properties, List<String> required) {
        ObjectNode tool = json.createObjectNode();
        tool.put("name", name);
        tool.put("description", description);
        ObjectNode schema = json.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", json.valueToTree(properties));
        schema.set("required", json.valueToTree(required));
        tool.set("inputSchema", schema);
        return tool;
    }

    private ObjectNode toolsCall(JsonNode params, String ownerId) {
        String name = text(params, "name");
        JsonNode args = params == null ? null : params.get("arguments");
        Object payload = switch (name) {
            case "search" -> searchService.search(text(args, "query"), textOr(args, "type", "all"), null, null, ownerId);
            case "list_notes" -> noteService.findAll(text(args, "folder"), text(args, "tag"), null, null, null, false, false, ownerId);
            case "get_note" -> noteService.findById(text(args, "id"), ownerId);
            case "create_note" -> noteService.create(noteInput(args), ownerId);
            case "update_note" -> noteService.update(text(args, "id"), noteInput(args), ownerId);
            case "list_todos" -> todoService.findAll(bool(args, "completed"), text(args, "tag"), null, null, false, false, ownerId);
            case "get_todo" -> todoService.findById(text(args, "id"), ownerId);
            case "create_todo" -> todoService.create(todoInput(args), ownerId);
            case "toggle_todo" -> todoService.toggleComplete(text(args, "id"), ownerId);
            default -> throw new IllegalArgumentException("Unknown tool: " + name);
        };
        return textResult(payload);
    }

    private ObjectNode resourcesList(String ownerId) {
        ArrayNode resources = json.createArrayNode();
        for (Note note : noteService.findAll(null, null, null, null, null, false, false, ownerId)) {
            ObjectNode resource = json.createObjectNode();
            resource.put("uri", "note://" + note.getId());
            resource.put("name", note.getTitle());
            resource.put("mimeType", "text/markdown");
            resources.add(resource);
        }
        ObjectNode result = json.createObjectNode();
        result.set("resources", resources);
        return result;
    }

    private ObjectNode resourcesRead(JsonNode params, String ownerId) {
        String uri = text(params, "uri");
        if (uri == null || !uri.startsWith("note://")) {
            throw new IllegalArgumentException("Unsupported resource uri");
        }
        Note note = noteService.findById(uri.substring("note://".length()), ownerId);
        ObjectNode content = json.createObjectNode();
        content.put("uri", uri);
        content.put("mimeType", "text/markdown");
        content.put("text", "# " + note.getTitle() + "\n\n" + (note.getContent() == null ? "" : note.getContent()));
        ObjectNode result = json.createObjectNode();
        result.set("contents", json.createArrayNode().add(content));
        return result;
    }

    private ObjectNode textResult(Object payload) {
        ObjectNode result = json.createObjectNode();
        ArrayNode content = json.createArrayNode();
        ObjectNode text = json.createObjectNode();
        text.put("type", "text");
        text.put("text", JSON_SAFE(payload));
        content.add(text);
        result.set("content", content);
        return result;
    }

    private String JSON_SAFE(Object payload) {
        if (payload instanceof SearchResult result) {
            return json.writeValueAsString(Map.of(
                    "notes", result.getNotes() == null ? List.of() : result.getNotes().stream().map(this::noteSummary).toList(),
                    "todos", result.getTodos() == null ? List.of() : result.getTodos().stream().map(this::todoSummary).toList()
            ));
        }
        if (payload instanceof List<?> list) {
            return json.writeValueAsString(list.stream().map(item -> {
                if (item instanceof Note note) {
                    return noteSummary(note);
                }
                if (item instanceof Todo todo) {
                    return todoSummary(todo);
                }
                return item;
            }).toList());
        }
        if (payload instanceof Note note) {
            return json.writeValueAsString(noteDetail(note));
        }
        if (payload instanceof Todo todo) {
            return json.writeValueAsString(todoDetail(todo));
        }
        return json.writeValueAsString(payload);
    }

    private Map<String, Object> noteSummary(Note note) {
        return Map.of(
                "id", note.getId(),
                "title", nullToEmpty(note.getTitle()),
                "folder", nullToEmpty(note.getFolder()),
                "updatedAt", String.valueOf(note.getUpdatedAt())
        );
    }

    private Map<String, Object> noteDetail(Note note) {
        return Map.of(
                "id", note.getId(),
                "title", nullToEmpty(note.getTitle()),
                "content", nullToEmpty(note.getContent()),
                "folder", nullToEmpty(note.getFolder()),
                "tags", note.getTags() == null ? List.of() : note.getTags()
        );
    }

    private Map<String, Object> todoSummary(Todo todo) {
        return Map.of(
                "id", todo.getId(),
                "title", nullToEmpty(todo.getTitle()),
                "completed", todo.isCompleted(),
                "updatedAt", String.valueOf(todo.getUpdatedAt())
        );
    }

    private Map<String, Object> todoDetail(Todo todo) {
        return Map.of(
                "id", todo.getId(),
                "title", nullToEmpty(todo.getTitle()),
                "description", nullToEmpty(todo.getDescription()),
                "completed", todo.isCompleted(),
                "tags", todo.getTags() == null ? List.of() : todo.getTags()
        );
    }

    private NoteInput noteInput(JsonNode args) {
        NoteInput input = new NoteInput();
        input.setTitle(text(args, "title"));
        input.setContent(textOr(args, "content", ""));
        input.setFolder(textOr(args, "folder", "default"));
        if (args != null && args.get("tags") != null && args.get("tags").isArray()) {
            input.setTags(json.convertValue(args.get("tags"), json.getTypeFactory().constructCollectionType(List.class, String.class)));
        }
        return input;
    }

    private TodoInput todoInput(JsonNode args) {
        TodoInput input = new TodoInput();
        input.setTitle(text(args, "title"));
        input.setDescription(textOr(args, "description", ""));
        return input;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asString(null);
    }

    private static String textOr(JsonNode node, String field, String fallback) {
        String value = text(node, field);
        return value == null ? fallback : value;
    }

    private static Boolean bool(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asBoolean();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
