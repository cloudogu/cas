package de.triology.cas.poc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Getter
public class PersonalAccessToken {
    private final Long id;
    private final String username;
    private final String token;
    private String scope = "read";
    private final LocalDateTime issuedAt;
    private final LocalDateTime expiresAt;
}
