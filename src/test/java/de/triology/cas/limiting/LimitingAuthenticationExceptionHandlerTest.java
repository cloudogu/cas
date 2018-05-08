package de.triology.cas.limiting;

import org.jasig.cas.authentication.AuthenticationException;
import org.junit.Test;
import org.springframework.binding.message.MessageContext;

import javax.security.auth.login.FailedLoginException;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LimitingAuthenticationExceptionHandlerTest {

    private final MessageContext messageContext = mock(MessageContext.class);

    @Test
    public void shouldHandleTemporayLock() {
        String handle = new LimitingAuthenticationExceptionHandler().handle(new AuthenticationException(singletonMap("", AccountTemporarilyLockedException.class)), messageContext);
        assertEquals("AccountTemporarilyLockedException", handle);
        verify(messageContext).addMessage(any());
    }

    @Test
    public void shouldForwardOtherExceptions() {
        String handle = new LimitingAuthenticationExceptionHandler().handle(new AuthenticationException(singletonMap("", FailedLoginException.class)), messageContext);
        assertEquals("FailedLoginException", handle);
        verify(messageContext).addMessage(any());
    }
}
