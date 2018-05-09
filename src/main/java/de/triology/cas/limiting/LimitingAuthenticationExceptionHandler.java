package de.triology.cas.limiting;

import org.jasig.cas.authentication.AuthenticationException;
import org.jasig.cas.web.flow.AuthenticationExceptionHandler;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.binding.message.MessageContext;

public class LimitingAuthenticationExceptionHandler extends AuthenticationExceptionHandler {
    @Override
    public String handle(AuthenticationException e, MessageContext messageContext) {
        for (final Class<? extends Exception> handlerError : e.getHandlerErrors().values()) {
            if (handlerError != null && handlerError.equals(AccountTemporarilyLockedException.class)) {
                final String messageCode = "authenticationFailure." + handlerError.getSimpleName();
                messageContext.addMessage(new MessageBuilder().error().code(messageCode).build());
                return handlerError.getSimpleName();
            }
        }
        return super.handle(e, messageContext);
    }
}
