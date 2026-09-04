package de.triology.cas.pat.controller;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

import de.triology.cas.pat.model.CreatePATRequest;
import de.triology.cas.pat.model.CreatePATResponse;
import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.service.PATService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * HTTP controller exposing owner-scoped PAT operations to the trusted User Management backend.
 */
@RestController
@RequestMapping("/api/users/{userId}/pats")
public class PATController {
    private final PATService service;

    public PATController(PATService service) {
        this.service = service;
    }

    /**
     * Creates a PAT for the delegated user while auditing the authenticated service account.
     */
    @PostMapping
    public ResponseEntity<CreatePATResponse> create(
            @PathVariable String userId,
            @Valid @RequestBody CreatePATRequest request,
            Principal principal) {
        CreatePATResponse response = service.create(userId, request, principal.getName());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(response);
    }

    /**
     * Returns the value provided by findAll.
     */
    @GetMapping
    public List<PATMetadata> findAll(@PathVariable String userId) {
        return service.findAll(userId);
    }

    /**
     * Returns the value provided by findOne.
     */
    @GetMapping("/{id}")
    public PATMetadata findOne(@PathVariable String userId, @PathVariable UUID id) {
        return service.findOne(userId, id);
    }

    /**
     * Deletes a PAT for the delegated user while auditing the authenticated service account.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String userId,
            @PathVariable UUID id,
            Principal principal) {
        service.delete(userId, id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
