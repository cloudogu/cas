package de.triology.cas.ldap.resolvers;

import org.ldaptive.*;
import org.pac4j.core.profile.UserProfile;

public class UserUpdater implements SingleUserUpdater {
    private final String baseDN;
    private final ConnectionFactory connectionFactory;
    private final String uidAttribute = "uid";

    public UserUpdater(final String baseDN, final ConnectionFactory connectionFactory){
        this.baseDN = baseDN;
        this.connectionFactory = connectionFactory;
    }
    @Override
    public void updateUser(UserProfile user) {
        try {
            final ModifyOperation modify = new ModifyOperation(this.connectionFactory);
            final ModifyResponse res = modify.execute(ModifyRequest.builder()
                    .dn(this.uidAttribute + "=" + user.getAttribute("preferred_username") + "," + this.baseDN)
                    .modifications(
                            replace("mail", (String)user.getAttribute("email")),
                            replace("sn", (String)user.getAttribute("family_name")),
                            replace("cn", (String)user.getAttribute("given_name")),
                            replace("displayName", (String)user.getAttribute("name"))
                    )
                    .build());
            if (!res.isSuccess()){
                throw new Exception(res.getDiagnosticMessage());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AttributeModification replace(final String type, final String newValue) {
        return new AttributeModification(AttributeModification.Type.REPLACE, new LdapAttribute(type, newValue));
    }
}
