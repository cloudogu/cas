package de.triology.cas.ldap.resolvers;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.apereo.cas.authentication.principal.Principal;
import org.ldaptive.*;

import javax.naming.directory.SearchControls;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;



// ALL-USERS: ldapsearch -x -b "ou=People,o=ces.local,dc=cloudogu,dc=com" "(objectClass=person)"

@Slf4j
public class UserResolver implements AllUserResolver {
    private final String baseDN;
    private final SearchControls searchControls;
    private SearchScope searchScope;
    private final ConnectionFactory connectionFactory;
    private final String uidAttribute = "uid";


    public UserResolver(String baseDN, ConnectionFactory connectionFactory) {
        this.baseDN = baseDN;
        this.searchControls = new SearchControls();
        this.searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        this.searchScope = SearchScope.SUBTREE;

        this.connectionFactory = connectionFactory;
    }

    public Set<String> resolveAllUserNames() {
        log.trace("resolveAllUserNames");
        final SearchResponse response;
        try {
            final SearchRequest request = createRequest();
            log.trace("resolveAllUserNames - request: {}", request);
            response = new SearchOperation(connectionFactory).execute(request);
        } catch (final LdapException e) {
            log.trace("resolveAllUserNames - error: {}", e.getMessage());
            throw new RuntimeException("Failed executing LDAP query ", e);
        }
        log.trace("got response {}", response);
        log.trace("got response entries{}", response.getEntries());
        final Set<String> users = new HashSet<>();
        for (final LdapEntry entry : response.getEntries()) {
            String group = extractGroupName(entry);
            log.trace("added user {} to attribute map", group);
            users.add(group);
        }
        return users;
    }

    private String extractGroupName(LdapEntry entry) {
        return entry.getAttribute(uidAttribute).getStringValue();
    }

    private SearchRequest createRequest() {
        val request = new SearchRequest();
        request.setBaseDn(this.baseDN);
        request.setReturnAttributes(uidAttribute);
        request.setFilter("(&(objectClass=person)(external=TRUE))");
        request.setSearchScope(searchScope);
        request.setSizeLimit(Math.toIntExact(this.searchControls.getCountLimit()));
        request.setTimeLimit(Duration.ofMillis(this.searchControls.getTimeLimit()));
        return request;
    }

}
