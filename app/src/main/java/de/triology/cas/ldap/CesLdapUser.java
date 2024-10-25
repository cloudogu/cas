package de.triology.cas.ldap;

import lombok.Getter;
import lombok.Setter;
import org.ldaptive.LdapEntry;
import org.pac4j.core.profile.UserProfile;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
public class CesLdapUser {
    //Fixme mappings
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

    public CesLdapUser(String uid, String givenName, String familyName, String displayName, String mail, boolean external) {
        this.uid = uid;
        this.givenName = givenName;
        this.familyName = familyName;
        this.displayName = displayName;
        this.mail = mail;
        this.external = external;
        this.groups = new HashSet<>();
    }

    public static CesLdapUser UserFromEntry(LdapEntry entry) {
        String uid = entry.getAttribute(CesLdapUser.UidAttribute).getStringValue();
        String givenName = entry.getAttribute(CesLdapUser.GivenNameAttribute).getStringValue();
        String familyName = entry.getAttribute(CesLdapUser.SnAttribute).getStringValue();
        String displayName = entry.getAttribute(CesLdapUser.DisplayNameAttribute).getStringValue();
        String mail = entry.getAttribute(CesLdapUser.MailAttribute).getStringValue();

        String externalString = entry.getAttribute(CesLdapUser.ExternalAttribute).getStringValue();
        boolean external = UserManager.LDAP_TRUE.equals(externalString);

        CesLdapUser user = new CesLdapUser(uid, givenName, familyName, displayName, mail, external);

        if (entry.getAttribute(CesLdapUser.MemberOfAttribute) != null) {
            for (String value : entry.getAttribute(CesLdapUser.MemberOfAttribute).getStringValues()) {
                String group = normalizeName(value);
                user.groups.add(group);
            }
        }

        return user;
    }

    public static CesLdapUser UserFromProfile(UserProfile profile) {

        String uid = (String) profile.getAttribute("preferred_username");
        String givenName = (String) profile.getAttribute("given_name");
        String familyName = (String) profile.getAttribute("family_name");
        String displayName = (String) profile.getAttribute("name");
        String mail = (String) profile.getAttribute("email");

        return new CesLdapUser(uid, givenName, familyName, displayName, mail, true);
    }

    //FIXME duplication from MemberOfGroupResolver
    private static String normalizeName(String value) {
        return extractNameFromDn(value);
    }

    private static String extractNameFromDn(String dn) {
        String result = dn;
        int eqindex = dn.indexOf('=');
        int coindex = dn.indexOf(',');
        if (eqindex > 0 && (coindex < 0 || eqindex < coindex) && dn.length() > eqindex + 1) {
            dn = dn.substring(eqindex + 1);
            coindex = dn.indexOf(',');
            if (coindex > 0) {
                result = dn.substring(0, coindex);
            } else {
                result = dn;
            }
        }
        return result;
    }
}
