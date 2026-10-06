package de.triology.cas.pat.controller;

import de.triology.cas.pat.service.PATService;
import java.util.List;
import java.util.Map;
import org.apereo.cas.authentication.principal.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** Returns the principal authenticated by the PAT supplied with this request. */
@RestController
@lombok.extern.slf4j.Slf4j
public class PATPrincipalController {
    private final PATService patService;

    public PATPrincipalController(PATService patService) {
        this.patService = patService;
    }

    @GetMapping("/api/pats/validate")
    public ResponseEntity<PrincipalResponse> validate(Authentication authentication,
                                                @RequestParam("scope") String scope) {
        if (scope == null || scope.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scope is required");
        }

        var principal = (Principal) authentication.getPrincipal();
        var scopes = principal.getAttributes().get(PATService.PAT_SCOPE_ATTRIBUTE);
        if (scopes == null || scopes.size() != 1 || !(scopes.getFirst() instanceof String patScope)
                || !patService.isScopeAllowed(patScope, scope)) {
            LOGGER.warn("event=pat_validate result=unauthorized reason=scope_denied principal={} requestedScope={} patScopes={}", principal.getId(), scope, scopes);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PAT does not allow the requested scope");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new PrincipalResponse(principal.getId(), principal.getAttributes()));
    }

    public record PrincipalResponse(String id, Map<String, List<Object>> attributes) { }
}
