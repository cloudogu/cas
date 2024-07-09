package de.triology.cas.logging;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(
        name = "DefaultMappingPasswordRewritePolicy",
        category = "Core",
        elementType = "rewritePolicy",
        printObject = true
)
/*
 * Password rewriter for class org.springframework.binding.mapping.impl.DefaultMapping.
 */
public final class DefaultMappingPasswordRewritePolicy extends AbstractCASPasswordRewritePolicy {
    private static final String PARAMETER_PASSWORD_TEXT = "parameter:'password'";
    private static final String REPLACEMENT_MESSAGE = "this log line contained sensitive data and was removed";

    @PluginFactory
    public static DefaultMappingPasswordRewritePolicy createPolicy() {
        return new DefaultMappingPasswordRewritePolicy();
    }

    private DefaultMappingPasswordRewritePolicy() {
        //
    }

    @Override
    protected String getPasswordFlag() {
        return PARAMETER_PASSWORD_TEXT;
    }

    @Override
    protected String replacePasswordValue(String originMessage) {
        String modifiedMessage = null;
        if (originMessage != null) {
            return REPLACEMENT_MESSAGE;
        }

        return modifiedMessage;
    }
}