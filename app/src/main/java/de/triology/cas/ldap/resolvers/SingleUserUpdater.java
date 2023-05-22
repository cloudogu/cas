package de.triology.cas.ldap.resolvers;

import org.pac4j.core.profile.UserProfile;

public interface SingleUserUpdater {
    void updateUser(UserProfile user);
}
