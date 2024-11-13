package de.triology.cas.ldap;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.ldaptive.LdapEntry;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@EqualsAndHashCode
public class CesInternalLdapUser {
    public static final String[] ObjectClasses = new String[]{"top", "person", "inetOrgPerson", "organizationalPerson", "cesperson"};

    public static final String UidAttribute = "uid";
    public static final String CnAttribute = "cn";
    public static final String SnAttribute = "sn";
    public static final String GivenNameAttribute = "givenname";
    public static final String DisplayNameAttribute = "displayName";
    public static final String MailAttribute = "mail";
    public static final String ExternalAttribute = "external";
    public static final String MemberOfAttribute = "memberOf";

    private String uid;
    private String givenName;
    private String familyName;
    private String displayName;
    private String mail;
    private boolean external;
    private Set<String> groups;

    public CesInternalLdapUser(String uid, String givenName, String familyName, String displayName, String mail, boolean external) {
        this.uid = uid;
        this.givenName = givenName;
        this.familyName = familyName;
        this.displayName = displayName;
        this.mail = mail;
        this.external = external;
        this.groups = new HashSet<>();
    }

    public static CesInternalLdapUser UserFromEntry(LdapEntry entry) {
        String uid = entry.getAttribute(CesInternalLdapUser.UidAttribute).getStringValue();
        String givenName = entry.getAttribute(CesInternalLdapUser.GivenNameAttribute).getStringValue();
        String familyName = entry.getAttribute(CesInternalLdapUser.SnAttribute).getStringValue();
        String displayName = entry.getAttribute(CesInternalLdapUser.DisplayNameAttribute).getStringValue();
        String mail = entry.getAttribute(CesInternalLdapUser.MailAttribute).getStringValue();

        String externalString = entry.getAttribute(CesInternalLdapUser.ExternalAttribute).getStringValue();
        boolean external = UserManager.LDAP_TRUE.equals(externalString);

        CesInternalLdapUser user = new CesInternalLdapUser(uid, givenName, familyName, displayName, mail, external);

        if (entry.getAttribute(CesInternalLdapUser.MemberOfAttribute) != null) {
            for (String value : entry.getAttribute(CesInternalLdapUser.MemberOfAttribute).getStringValues()) {
                String group = Util.extractGroupNameFromDn(value);
                user.groups.add(group);
            }
        }

        return user;
    }


}
