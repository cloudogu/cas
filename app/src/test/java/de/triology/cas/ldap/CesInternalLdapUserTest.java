package de.triology.cas.ldap;

import org.junit.jupiter.api.Test;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CesInternalLdapUserTest {

    private static LdapEntry baseEntry() {
        LdapEntry entry = new LdapEntry();
        entry.addAttributes(new LdapAttribute(CesInternalLdapUser.UidAttribute, "test"));
        entry.addAttributes(new LdapAttribute(CesInternalLdapUser.GivenNameAttribute, "Test"));
        entry.addAttributes(new LdapAttribute(CesInternalLdapUser.SnAttribute, "User"));
        entry.addAttributes(new LdapAttribute(CesInternalLdapUser.DisplayNameAttribute, "Test User"));
        entry.addAttributes(new LdapAttribute(CesInternalLdapUser.MailAttribute, "test@user.de"));
        return entry;
    }

    @Test
    void userFromEntry_withMemberOfDns_extractsGroupNames() {
        LdapEntry entry = baseEntry();
        entry.addAttributes(new LdapAttribute(CesInternalLdapUser.ExternalAttribute, "TRUE"));
        entry.addAttributes(new LdapAttribute(CesInternalLdapUser.MemberOfAttribute,
                "cn=Admins,ou=Groups,dc=example,dc=com", "cn=Users,ou=Groups,dc=example,dc=com"));

        CesInternalLdapUser user = CesInternalLdapUser.UserFromEntry(entry);

        assertEquals("test", user.getUid());
        assertTrue(user.isExternal());
        assertEquals(2, user.getGroups().size());
        assertTrue(user.getGroups().contains("Admins"));
        assertTrue(user.getGroups().contains("Users"));
    }

    @Test
    void userFromEntry_withoutMemberOfAttribute_hasEmptyGroups() {
        LdapEntry entry = baseEntry();
        entry.addAttributes(new LdapAttribute(CesInternalLdapUser.ExternalAttribute, "FALSE"));
        // no MemberOfAttribute added at all

        CesInternalLdapUser user = CesInternalLdapUser.UserFromEntry(entry);

        assertFalse(user.isExternal());
        assertTrue(user.getGroups().isEmpty());
    }

    @Test
    void userFromEntry_externalAttributeNotTrue_isNotExternal() {
        LdapEntry entry = baseEntry();
        entry.addAttributes(new LdapAttribute(CesInternalLdapUser.ExternalAttribute, "someOtherValue"));

        CesInternalLdapUser user = CesInternalLdapUser.UserFromEntry(entry);

        assertFalse(user.isExternal());
    }
}
