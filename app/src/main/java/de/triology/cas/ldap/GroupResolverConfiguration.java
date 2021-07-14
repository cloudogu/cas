package de.triology.cas.ldap;

import org.apereo.cas.configuration.CasConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.naming.directory.SearchControls;
import java.util.ArrayList;
import java.util.List;

@Configuration("GroupResolverConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class GroupResolverConfiguration {

    @Bean
    @RefreshScope
    CombinedGroupResolver combinedGroupResolver() {
        return new CombinedGroupResolver(groupResolvers());
    }

    @Bean
    List<GroupResolver> groupResolvers() {
        List<GroupResolver> groupResolvers = new ArrayList<>(2);
        groupResolvers.add(memberOfGroupResolver());
//        groupResolvers.add(memberGroupResolver());

        return groupResolvers;
    }

    @Bean
    MemberGroupResolver memberGroupResolver() {
        return new MemberGroupResolver();
    }

    @Bean
    MemberOfGroupResolver memberOfGroupResolver() {
        return new MemberOfGroupResolver();
    }

    @Bean
    SearchControls searchControls() {
        var searchControls = new SearchControls();
        searchControls.setSearchScope(2);

        return searchControls;
    }
}
