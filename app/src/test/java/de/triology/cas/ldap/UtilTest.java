package de.triology.cas.ldap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilTest {

    @Test
    void extractGroupNameFromDn_singleRdn() {
        assertEquals("Admins", Util.extractGroupNameFromDn("cn=Admins,ou=Groups,dc=example,dc=com"));
    }

    @Test
    void extractGroupNameFromDn_noTrailingComma() {
        // No comma after the value: the whole remainder after '=' is the group name.
        assertEquals("Admins", Util.extractGroupNameFromDn("cn=Admins"));
    }

    @Test
    void extractGroupNameFromDn_noEqualsSign() {
        // No '=' at all: the input is returned unchanged.
        assertEquals("not-a-dn", Util.extractGroupNameFromDn("not-a-dn"));
    }

    @Test
    void extractGroupNameFromDn_commaBeforeEquals() {
        // A comma occurring before the first '=' means this isn't a leading RDN; returned unchanged.
        assertEquals("foo,cn=bar", Util.extractGroupNameFromDn("foo,cn=bar"));
    }

    @Test
    void extractGroupNameFromDn_equalsAtStart() {
        // '=' at index 0 is not a valid "key=value" RDN prefix; returned unchanged.
        assertEquals("=Admins,ou=Groups", Util.extractGroupNameFromDn("=Admins,ou=Groups"));
    }

    @Test
    void extractGroupNameFromDn_nothingAfterEquals() {
        // Nothing after the trailing '=': returned unchanged.
        assertEquals("cn=", Util.extractGroupNameFromDn("cn="));
    }

    @Test
    void extractGroupNameFromDn_emptyString() {
        assertEquals("", Util.extractGroupNameFromDn(""));
    }
}
