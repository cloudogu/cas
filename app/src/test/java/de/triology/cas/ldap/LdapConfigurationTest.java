package de.triology.cas.ldap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apereo.cas.authentication.LdapAuthenticationHandler;
import org.apereo.cas.configuration.model.support.ldap.LdapAuthenticationProperties;
import org.junit.jupiter.api.Test;
import org.ldaptive.auth.Authenticator;

import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Exercises the private, pure-logic helper methods of {@link LdapConfiguration} directly via
 * reflection, without booting a Spring context. The helpers that delegate to real CAS/ldaptive
 * static factories (e.g. {@code LdapUtils.newLdaptivePooledConnectionFactory}) are intentionally
 * left untested here, since exercising them would require either a real/embedded LDAP server or
 * assumptions about their exact expected input format that aren't verifiable without running the
 * build.
 */
class LdapConfigurationTest {

    private static void invokePrivate(String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method method = LdapConfiguration.class.getDeclaredMethod(name, paramTypes);
        method.setAccessible(true);
        method.invoke(new LdapConfiguration(), args);
    }

    @Test
    void configureDNAttributes_setsPrincipalDnAttributeName_whenNotBlank() throws Exception {
        LdapAuthenticationHandler handler = mock(LdapAuthenticationHandler.class);
        LdapAuthenticationProperties properties = new LdapAuthenticationProperties();
        properties.setCollectDnAttribute(true);
        properties.setPrincipalDnAttributeName("distinguishedName");

        invokePrivate("configureDNAttributes",
                new Class[]{LdapAuthenticationHandler.class, LdapAuthenticationProperties.class},
                handler, properties);

        verify(handler).setCollectDnAttribute(true);
        verify(handler).setPrincipalDnAttributeName("distinguishedName");
    }

    @Test
    void configureDNAttributes_skipsPrincipalDnAttributeName_whenBlank() throws Exception {
        LdapAuthenticationHandler handler = mock(LdapAuthenticationHandler.class);
        LdapAuthenticationProperties properties = new LdapAuthenticationProperties();
        properties.setCollectDnAttribute(false);
        properties.setPrincipalDnAttributeName("   ");

        invokePrivate("configureDNAttributes",
                new Class[]{LdapAuthenticationHandler.class, LdapAuthenticationProperties.class},
                handler, properties);

        verify(handler).setCollectDnAttribute(false);
        verify(handler, never()).setPrincipalDnAttributeName(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void configurePrincipalAttributeId_setsId_whenNotBlank() throws Exception {
        LdapAuthenticationHandler handler = mock(LdapAuthenticationHandler.class);
        LdapAuthenticationProperties properties = new LdapAuthenticationProperties();
        properties.setPrincipalAttributeId("uid");

        invokePrivate("configurePrincipalAttributeId",
                new Class[]{LdapAuthenticationHandler.class, LdapAuthenticationProperties.class},
                handler, properties);

        verify(handler).setPrincipalIdAttribute("uid");
    }

    @Test
    void configurePrincipalAttributeId_doesNothing_whenBlank() throws Exception {
        LdapAuthenticationHandler handler = mock(LdapAuthenticationHandler.class);
        LdapAuthenticationProperties properties = new LdapAuthenticationProperties();
        properties.setPrincipalAttributeId("");

        invokePrivate("configurePrincipalAttributeId",
                new Class[]{LdapAuthenticationHandler.class, LdapAuthenticationProperties.class},
                handler, properties);

        verify(handler, never()).setPrincipalIdAttribute(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void configureCredentialCriteria_doesNothing_whenBlank() throws Exception {
        LdapAuthenticationHandler handler = mock(LdapAuthenticationHandler.class);
        LdapAuthenticationProperties properties = new LdapAuthenticationProperties();
        properties.setCredentialCriteria(null);

        invokePrivate("configureCredentialCriteria",
                new Class[]{LdapAuthenticationHandler.class, LdapAuthenticationProperties.class},
                handler, properties);

        verify(handler, never()).setCredentialSelectionPredicate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void configurePasswordPolicy_doesNothing_whenDisabled() throws Exception {
        LdapAuthenticationHandler handler = mock(LdapAuthenticationHandler.class);
        LdapAuthenticationProperties properties = new LdapAuthenticationProperties();
        properties.getPasswordPolicy().setEnabled(false);
        Authenticator authenticator = mock(Authenticator.class);
        Multimap<String, Object> attributes = ArrayListMultimap.create();

        invokePrivate("configurePasswordPolicy",
                new Class[]{LdapAuthenticationHandler.class, LdapAuthenticationProperties.class,
                        Authenticator.class, Multimap.class},
                handler, properties, authenticator, attributes);

        verify(handler, never()).setPasswordPolicyConfiguration(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void appendAdditionalAttributes_doesNothing_whenListEmpty() throws Exception {
        LdapAuthenticationProperties properties = new LdapAuthenticationProperties();
        Multimap<String, Object> attributes = ArrayListMultimap.create();
        attributes.put("mail", "test@example.com");

        invokePrivate("appendAdditionalAttributes",
                new Class[]{LdapAuthenticationProperties.class, Multimap.class},
                properties, attributes);

        // additionalAttributes defaults to empty: the multimap must be left untouched.
        org.junit.jupiter.api.Assertions.assertEquals(1, attributes.size());
        org.junit.jupiter.api.Assertions.assertTrue(attributes.containsEntry("mail", "test@example.com"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void createsAndInitializesHandlerWithConfiguredAttributes(boolean policyEnabled) {
        var properties = new org.apereo.cas.configuration.CasConfigurationProperties();
        var ldap = new LdapAuthenticationProperties();
        ldap.setName("test-ldap");
        ldap.setPrincipalAttributeList(java.util.List.of("mail"));
        ldap.setAdditionalAttributes(java.util.List.of("cn"));
        ldap.setPrincipalAttributeId("uid");
        ldap.setPrincipalDnAttributeName("dn");
        ldap.setCollectDnAttribute(true);
        ldap.setCredentialCriteria(".+");
        ldap.getPasswordPolicy().setEnabled(policyEnabled);
        properties.getAuthn().setLdap(java.util.List.of(ldap));
        var context = mock(org.springframework.context.ConfigurableApplicationContext.class);
        var resolver = mock(de.triology.cas.ldap.resolvers.CombinedGroupResolver.class);
        var authenticator = mock(Authenticator.class);
        var connections = mock(org.ldaptive.PooledConnectionFactory.class);
        var policy = mock(org.apereo.cas.authentication.support.password.PasswordPolicyContext.class);
        try (var utils = org.mockito.Mockito.mockStatic(org.apereo.cas.util.LdapUtils.class);
             var handlers = org.mockito.Mockito.mockConstruction(CesGroupAwareLdapAuthenticationHandler.class)) {
            utils.when(() -> org.apereo.cas.util.LdapUtils.newLdaptiveAuthenticator(ldap)).thenReturn(authenticator);
            utils.when(() -> org.apereo.cas.util.LdapUtils.newLdaptivePooledConnectionFactory(ldap)).thenReturn(connections);
            utils.when(() -> org.apereo.cas.util.LdapUtils.createLdapPasswordPolicyConfiguration(
                    org.mockito.ArgumentMatchers.eq(ldap.getPasswordPolicy()),
                    org.mockito.ArgumentMatchers.same(authenticator), org.mockito.ArgumentMatchers.any()))
                    .thenReturn(policy);

            var handler = new LdapConfiguration().cesGroupAwareLdapAuthenticationHandler(properties, context, resolver);

            org.junit.jupiter.api.Assertions.assertSame(handlers.constructed().getFirst(), handler);
            verify(handler).initialize();
            verify(handler).setPrincipalIdAttribute("uid");
            verify(handler).setPrincipalDnAttributeName("dn");
            verify(handler).setCollectDnAttribute(true);
            verify(handler).setCredentialSelectionPredicate(org.mockito.ArgumentMatchers.any());
            verify(handler).setPrincipalAttributeMap(org.mockito.ArgumentMatchers.argThat(
                    attributes -> attributes.containsKey("mail") && attributes.containsKey("cn")));
            if (policyEnabled) verify(handler).setPasswordPolicyConfiguration(policy);
            else verify(handler, never()).setPasswordPolicyConfiguration(org.mockito.ArgumentMatchers.any());
        }
    }

    @Test
    void createsCombinedResolverWithConfiguredConnectionFactory() {
        var properties = new org.apereo.cas.configuration.CasConfigurationProperties();
        var ldap = new LdapAuthenticationProperties();
        properties.getAuthn().setLdap(java.util.List.of(ldap));
        var connections = mock(org.ldaptive.PooledConnectionFactory.class);
        var configuration = new LdapConfiguration();
        org.springframework.test.util.ReflectionTestUtils.setField(configuration, "baseDN", "dc=example");
        org.springframework.test.util.ReflectionTestUtils.setField(configuration, "searchFilter", "(uid={user})");
        org.springframework.test.util.ReflectionTestUtils.setField(configuration, "groupAttribute", "memberOf");
        try (var utils = org.mockito.Mockito.mockStatic(org.apereo.cas.util.LdapUtils.class)) {
            utils.when(() -> org.apereo.cas.util.LdapUtils.newLdaptivePooledConnectionFactory(ldap)).thenReturn(connections);
            org.junit.jupiter.api.Assertions.assertNotNull(configuration.combinedGroupResolver(properties));
            utils.verify(() -> org.apereo.cas.util.LdapUtils.newLdaptivePooledConnectionFactory(ldap));
        }
    }

}
