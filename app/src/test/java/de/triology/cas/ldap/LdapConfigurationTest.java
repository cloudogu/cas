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
}
