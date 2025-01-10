package de.triology.cas.oidc.beans.delegation;

import org.apereo.cas.authentication.principal.Principal;

import java.util.ArrayList;
import java.util.List;

public class PrincipalGroups {

    private final static String groupsAttributeName = "groups";

    public static List<Object> getGroupsFromPrincipal(Principal principal) {
        List<Object> groups = principal.getAttributes().get(groupsAttributeName);
        if (groups == null) {
            groups = new ArrayList<>();
        }

        return groups;
    }

    public static void setGroupsInPrincipal(Principal principal, List<Object> groups) {
        principal.getAttributes().put(groupsAttributeName, groups);
    }


}
