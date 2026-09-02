package de.triology.cas.pat.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import de.triology.cas.pat.model.CreatePATRequest;
import de.triology.cas.pat.model.CreatePATResponse;
import de.triology.cas.pat.model.PATMetadata;
import de.triology.cas.pat.model.StoredPAT;
import de.triology.cas.pat.repository.PATRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements PAT creation, owner-scoped lookup and physical deletion.
 * It coordinates validation, secure generation, fingerprint persistence and audit events.
 */
public class PATService {
    private static final Logger AUDIT = LoggerFactory.getLogger("de.triology.cas.pat.audit");
    private static final int MAX_TEXT_LENGTH = 255;
    private static final int MAX_SCOPE_LENGTH = 1000;
    private static final String DEFAULT_SCOPE = "/*";

    private final PATRepository repository;
    private final SecurePATGenerator generator;
    private final Clock clock;

    /**
     * Creates the PAT application service.
     *
     * @param repository persistent PAT store
     * @param generator secure token generator
     * @param clock UTC-capable clock used for deterministic timestamps
     */
    public PATService(
            PATRepository repository,
            SecurePATGenerator generator,
            Clock clock) {
        this.repository = repository;
        this.generator = generator;
        this.clock = clock;
    }

    /**
     * Validates and generates a PAT, then stores only its irreversible fingerprint and metadata.
     *
     * @param userId delegated owner supplied by User Management
     * @param request requested metadata and optional expiration
     * @param actor authenticated technical caller
     * @return create response containing the cleartext token exactly once
     * @throws PATRequestException when request fields are invalid
     */
    public CreatePATResponse create(String userId, CreatePATRequest request, String actor) {
        String validatedUserId = requiredText("userId", userId);
        String displayName = request.displayName().strip();
        String scope = request.scope() == null || request.scope().isBlank()
                ? DEFAULT_SCOPE
                : request.scope().strip();
        if (scope.length() > MAX_SCOPE_LENGTH) {
            throw new PATRequestException("scope must not exceed " + MAX_SCOPE_LENGTH + " characters");
        }
        Instant createdAt = clock.instant();
        Instant expiresAt = validateExpiresAt(request.expiresAt(), createdAt);

        UUID id = UUID.randomUUID();
        GeneratedPAT generated = generator.generate();
        StoredPAT stored = new StoredPAT(
                id,
                validatedUserId,
                displayName,
                generated.fingerprint(),
                createdAt,
                expiresAt,
                scope);
        try {
            repository.insert(stored);
        } catch (PATStorageUnavailableException e) {
            AUDIT.error("event=pat_create userId={} principal={} result=storage_unavailable", validatedUserId, actor);
            throw e;
        }
        AUDIT.info("event=pat_created patId={} userId={} principal={} result=success", id, validatedUserId, actor);
        return new CreatePATResponse(
                id, validatedUserId, displayName, generated.token(), createdAt, expiresAt, scope);
    }

    /**
     * Lists all non-sensitive PAT metadata belonging to one user.
     *
     * @param userId owner identifier
     * @return metadata ordered from newest to oldest
     */
    public List<PATMetadata> findAll(String userId) {
        return repository.findAllByUserId(requiredText("userId", userId));
    }

    /**
     * Retrieves one PAT metadata record within the supplied owner boundary.
     *
     * @param userId owner identifier
     * @param id token record identifier
     * @return matching non-sensitive metadata
     * @throws PATNotFoundException when the record is absent or belongs to another user
     */
    public PATMetadata findOne(String userId, UUID id) {
        return repository.findByUserIdAndId(requiredText("userId", userId), id)
                .orElseThrow(PATNotFoundException::new);
    }

    /**
     * Physically deletes one PAT within the supplied owner boundary.
     *
     * @param userId owner identifier
     * @param id token record identifier
     * @param actor authenticated technical caller
     * @throws PATNotFoundException when the record is absent or belongs to another user
     */
    public void delete(String userId, UUID id, String actor) {
        String validatedUserId = requiredText("userId", userId);
        if (!repository.deleteByUserIdAndId(validatedUserId, id)) {
            AUDIT.warn("event=pat_delete patId={} userId={} principal={} result=not_found", id, validatedUserId, actor);
            throw new PATNotFoundException();
        }
        AUDIT.info("event=pat_deleted patId={} userId={} principal={} result=success", id, validatedUserId, actor);
    }

    /**
     * Trims and validates a required API text field.
     *
     * @param field field name used in validation errors
     * @param value unvalidated value
     * @return trimmed value
     * @throws PATRequestException when missing, blank or longer than 255 characters
     */
    private String requiredText(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new PATRequestException(field + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_TEXT_LENGTH) {
            throw new PATRequestException(field + " must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
        return trimmed;
    }

    /**
     * Verifies that an optional expiration lies in the future.
     *
     * @param expiresAt expiration timestamp, or {@code null}
     * @param createdAt creation time used as lower bound
     * @return validated expiration or {@code null}
     * @throws PATRequestException when the expiration is not in the future
     */
    private Instant validateExpiresAt(Instant expiresAt, Instant createdAt) {
        if (expiresAt != null && !expiresAt.isAfter(createdAt)) {
            throw new PATRequestException("expiresAt must be in the future");
        }
        return expiresAt;
    }
}
