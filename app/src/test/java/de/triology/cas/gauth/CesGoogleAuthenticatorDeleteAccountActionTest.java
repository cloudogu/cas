package de.triology.cas.gauth;

import org.apereo.cas.authentication.DefaultAuthenticationBuilder;
import org.apereo.cas.authentication.OneTimeTokenAccount;
import org.apereo.cas.authentication.principal.PrincipalFactoryUtils;
import org.apereo.cas.gauth.credential.GoogleAuthenticatorTokenCredential;
import org.apereo.cas.gauth.token.GoogleAuthenticatorToken;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialRepository;
import org.apereo.cas.otp.repository.credentials.OneTimeTokenCredentialValidator;
import org.apereo.cas.web.flow.CasWebflowConstants;
import org.apereo.cas.web.support.WebUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.test.MockRequestContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CesGoogleAuthenticatorDeleteAccountActionTest {
    private static final long ACCOUNT_ID = 1234L;
    private static final String USERNAME = "casuser";

    private InMemoryRepository repository;
    private OneTimeTokenCredentialValidator<GoogleAuthenticatorTokenCredential, GoogleAuthenticatorToken> validator;
    private CesGoogleAuthenticatorDeleteAccountAction action;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository();
        validator = mock(OneTimeTokenCredentialValidator.class);
        action = new CesGoogleAuthenticatorDeleteAccountAction(repository, validator);
        repository.save(account());
    }

    @Test
    void validOtpDeletesSelectedAccountAndReturnsSuccess() throws Throwable {
        when(validator.validate(any(), argThat(credential ->
            ACCOUNT_ID == credential.getAccountId() && "123456".equals(credential.getToken()))))
            .thenReturn(new GoogleAuthenticatorToken(123456, USERNAME));

        var event = action.execute(requestContext("true", "123456"));

        assertEvent(CasWebflowConstants.TRANSITION_ID_SUCCESS, event);
        assertNull(repository.get(ACCOUNT_ID));
    }

    @Test
    void validScratchCodeDeletesSelectedAccountAndReturnsSuccess() throws Throwable {
        when(validator.validate(any(), argThat(credential ->
            ACCOUNT_ID == credential.getAccountId() && "87654321".equals(credential.getToken()))))
            .thenReturn(new GoogleAuthenticatorToken(87654321, USERNAME));

        var event = action.execute(requestContext("true", "87654321"));

        assertEvent(CasWebflowConstants.TRANSITION_ID_SUCCESS, event);
        assertNull(repository.get(ACCOUNT_ID));
    }

    @Test
    void wrongTokenReturnsErrorLeavesAccountRegisteredAndExposesErrorMessage() throws Throwable {
        var context = requestContext("true", "000000");
        var event = action.execute(context);

        assertEvent(CasWebflowConstants.TRANSITION_ID_ERROR, event);
        assertEquals(ACCOUNT_ID, repository.get(ACCOUNT_ID).getId());
        assertTrue(context.getFlashScope().get("gauthDeleteDeviceError", Boolean.class));
        assertTrue(context.getMessageContext().hasErrorMessages());
    }

    @Test
    void emptyTokenReturnsErrorLeavesAccountRegisteredAndExposesErrorMessage() throws Throwable {
        var context = requestContext("true", "");
        var event = action.execute(context);

        assertEvent(CasWebflowConstants.TRANSITION_ID_ERROR, event);
        assertEquals(ACCOUNT_ID, repository.get(ACCOUNT_ID).getId());
        assertTrue(context.getFlashScope().get("gauthDeleteDeviceError", Boolean.class));
        assertTrue(context.getMessageContext().hasErrorMessages());
    }


    @Test
    void noAuthenticationReturnsError() throws Throwable {
        var flow = new Flow("login");
        flow.setApplicationContext(new StaticApplicationContext());
        var context = new MockRequestContext(flow);
        context.putRequestParameter("accountId", Long.toString(ACCOUNT_ID));
        context.putRequestParameter("validate", "true");
        context.putRequestParameter("token", "123456");

        var event = action.execute(context);

        assertEvent(CasWebflowConstants.TRANSITION_ID_ERROR, event);
        assertEquals(ACCOUNT_ID, repository.get(ACCOUNT_ID).getId());
        assertTrue(context.getFlashScope().get("gauthDeleteDeviceError", Boolean.class));
        assertTrue(context.getMessageContext().hasErrorMessages());
    }

    @Test
    void accountIdBlankReturnsError() throws Throwable {
        var flow = new Flow("login");
        flow.setApplicationContext(new StaticApplicationContext());
        var context = new MockRequestContext(flow);
        context.putRequestParameter("validate", "true");
        context.putRequestParameter("token", "123456");

        var event = action.execute(context);

        assertEvent(CasWebflowConstants.TRANSITION_ID_ERROR, event);
        assertEquals(ACCOUNT_ID, repository.get(ACCOUNT_ID).getId());
        assertTrue(context.getFlashScope().get("gauthDeleteDeviceError", Boolean.class));
        assertTrue(context.getMessageContext().hasErrorMessages());
    }

    @Test
    void accountIdNaNReturnsError() throws Throwable {
        var flow = new Flow("login");
        flow.setApplicationContext(new StaticApplicationContext());
        var context = new MockRequestContext(flow);
        context.putRequestParameter("accountId", "HelloWorld");
        context.putRequestParameter("validate", "true");
        context.putRequestParameter("token", "123456");

        var event = action.execute(context);

        assertEvent(CasWebflowConstants.TRANSITION_ID_ERROR, event);
        assertEquals(ACCOUNT_ID, repository.get(ACCOUNT_ID).getId());
        assertTrue(context.getFlashScope().get("gauthDeleteDeviceError", Boolean.class));
        assertTrue(context.getMessageContext().hasErrorMessages());
    }


    @Test
    void oldSingleStepDeletePostReturnsErrorAndDoesNotDelete() throws Throwable {
        var event = action.execute(requestContext("false", null));

        assertEvent(CasWebflowConstants.TRANSITION_ID_ERROR, event);
        assertEquals(ACCOUNT_ID, repository.get(ACCOUNT_ID).getId());
        verify(validator, never()).validate(any(), any());
    }

    private static void assertEvent(final String expectedId, final Event event) {
        assertEquals(expectedId, event.getId());
    }

    private static MockRequestContext requestContext(final String validate, final String token) throws Throwable {
        var flow = new Flow("login");
        flow.setApplicationContext(new StaticApplicationContext());
        var context = new MockRequestContext(flow);
        WebUtils.putAuthentication(
            DefaultAuthenticationBuilder.newInstance(PrincipalFactoryUtils.newPrincipalFactory().createPrincipal(USERNAME, Map.of())).build(),
            context);
        context.putRequestParameter("accountId", Long.toString(ACCOUNT_ID));
        context.putRequestParameter("validate", validate);
        if (token != null) {
            context.putRequestParameter("token", token);
        }
        return context;
    }

    private static OneTimeTokenAccount account() {
        var account = new OneTimeTokenAccount();
        account.setId(ACCOUNT_ID);
        account.setUsername(USERNAME);
        account.setName("Phone");
        account.setSecretKey("secret");
        return account;
    }

    private static final class InMemoryRepository implements OneTimeTokenCredentialRepository {
        private final Map<Long, OneTimeTokenAccount> accounts = new HashMap<>();

        @Override
        public OneTimeTokenAccount get(final long id) {
            return accounts.get(id);
        }

        @Override
        public OneTimeTokenAccount get(final String username, final long id) {
            return accounts.get(id);
        }

        @Override
        public Collection<? extends OneTimeTokenAccount> get(final String username) {
            return accounts.values().stream()
                .filter(account -> account.getUsername().equals(username))
                .toList();
        }

        @Override
        public Collection<? extends OneTimeTokenAccount> load() {
            return accounts.values();
        }

        @Override
        public OneTimeTokenAccount save(final OneTimeTokenAccount account) {
            accounts.put(account.getId(), account);
            return account;
        }

        @Override
        public OneTimeTokenAccount create(final String username) {
            var account = new OneTimeTokenAccount();
            account.setUsername(username);
            return account;
        }

        @Override
        public OneTimeTokenAccount update(final OneTimeTokenAccount account) {
            accounts.put(account.getId(), account);
            return account;
        }

        @Override
        public void deleteAll() {
            accounts.clear();
        }

        @Override
        public void delete(final String username) {
            accounts.values().removeIf(account -> account.getUsername().equals(username));
        }

        @Override
        public void delete(final long id) {
            accounts.remove(id);
        }

        @Override
        public long count() {
            return accounts.size();
        }

        @Override
        public long count(final String username) {
            return get(username).size();
        }
    }
}
