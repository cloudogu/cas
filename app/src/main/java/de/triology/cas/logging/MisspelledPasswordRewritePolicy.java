package de.triology.cas.logging;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(
        name = "MisspelledPasswordRewritePolicy",
        category = "Core",
        elementType = "rewritePolicy",
        printObject = true
)
/*
 * Password rewriter for class org.apereo.cas.web.flow.DefaultDelegatedClientIdentityProviderConfigurationProducer.
 */
public final class MisspelledPasswordRewritePolicy extends AbstractCASPasswordRewritePolicy {
    private static final String PARAMETER_PASSWORD_TEXT = "password=";

    @PluginFactory
    public static MisspelledPasswordRewritePolicy createPolicy() {
        return new MisspelledPasswordRewritePolicy();
    }

    private MisspelledPasswordRewritePolicy() {
        //
    }

    @Override
    protected String getPasswordFlag() {
        return PARAMETER_PASSWORD_TEXT;
    }

    @Override
    protected String replacePasswordValue(String originMessage) {
        String truncatedMessage = null;

        if (originMessage != null) {
            truncatedMessage = originMessage.replaceAll("password=\\[.*\\],\\s*exec", "password=[******], exec");
        }

        return truncatedMessage;
    }
}