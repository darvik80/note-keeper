package xyz.crearts.note.keeper.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.dto.SavedQueryInput;
import xyz.crearts.note.keeper.dto.SearchResult;
import xyz.crearts.note.keeper.model.SavedQuery;
import xyz.crearts.note.keeper.service.SearchService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResult search(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String priority) {
        return searchService.search(query, type, tags, priority);
    }

    @GetMapping("/queries")
    public List<SavedQuery> getSavedQueries() {
        return searchService.getSavedQueries();
    }

    @PostMapping("/queries")
    public ResponseEntity<SavedQuery> saveQuery(@Valid @RequestBody SavedQueryInput input) {
        SavedQuery query = searchService.saveQuery(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(query);
    }

    @DeleteMapping("/queries/{id}")
    public ResponseEntity<Void> deleteQuery(@PathVariable String id) {
        searchService.deleteQuery(id);
        return ResponseEntity.noContent().build();
    }
}
