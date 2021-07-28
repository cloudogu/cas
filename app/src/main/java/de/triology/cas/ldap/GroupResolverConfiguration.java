package de.triology.cas.ldap;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.util.LdapUtils;
import org.ldaptive.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.naming.directory.SearchControls;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Configuration("GroupResolverConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class GroupResolverConfiguration {

    @Autowired
    CasConfigurationProperties properties;

    @Value("${cas.authn.attributeRepository.ldap[0].attributes.groups}")
    private String groupAttribute;

    @Value("${cas.authn.attributeRepository.ldap[0].attributes.baseDn}")
    private String baseDN;

    /**
     * LDAP search filter.
     */
    @NotNull
    @Value("${cas.authn.ldap[0].search-filter}")
    private String searchFilter;

    @Bean
    @RefreshScope
    CombinedGroupResolver combinedGroupResolver() {
        return new CombinedGroupResolver(groupResolvers());
    }

    @Bean
    List<GroupResolver> groupResolvers() {
        List<GroupResolver> groupResolvers = new ArrayList<>(2);
        groupResolvers.add(memberGroupResolver());
        groupResolvers.add(memberOfGroupResolver());

        return groupResolvers;
    }

    @Bean
    MemberGroupResolver memberGroupResolver() {
        return new MemberGroupResolver(baseDN, searchControls(), searchPooledLdapConnectionFactory(), searchFilter);
    }

    @Bean
    MemberOfGroupResolver memberOfGroupResolver() {
        return new MemberOfGroupResolver(groupAttribute);
    }

    @Bean
    ConnectionFactory searchPooledLdapConnectionFactory() {
        var ldapProperties = properties.getAuthn().getLdap().get(0);
        return LdapUtils.newLdaptivePooledConnectionFactory(ldapProperties);
    }

    @Bean
    SearchControls searchControls() {
        var searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        return searchControls;
    }
}
