package de.triology.cas.ldap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CesLdapException}.
 */
class CesLdapExceptionTests {

    @Test
    void constructor_ShouldSetMessage() {
        // given
        String errorMessage = "An LDAP error occurred";

        // when
        CesLdapException exception = new CesLdapException(errorMessage);

        // then
        assertEquals(errorMessage, exception.getMessage(), "Exception message should match input message");
        assertNull(exception.getCause(), "Cause should be null when not provided");
    }

    @Test
    void constructor_ShouldSetMessageAndCause() {
        // given
        String errorMessage = "An LDAP error occurred";
        Throwable cause = new RuntimeException("Root cause");

        // when
        CesLdapException exception = new CesLdapException(errorMessage, cause);

        // then
        assertEquals(errorMessage, exception.getMessage(), "Exception message should match input message");
        assertEquals(cause, exception.getCause(), "Exception cause should match input cause");
    }
}
