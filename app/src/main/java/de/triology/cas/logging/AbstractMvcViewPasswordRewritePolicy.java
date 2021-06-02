package de.triology.cas.logging;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(
        name = "AbstractMvcViewPasswordRewritePolicy",
        category = "Core",
        elementType = "rewritePolicy",
        printObject = true
)
/*
 * Password rewriter for class org.springframework.webflow.mvc.view.AbstractMvcView.
 */
public final class AbstractMvcViewPasswordRewritePolicy extends AbstractCASPasswordRewritePolicy {
    private static final String PARAMETER_PASSWORD_TEXT = "password=";

    @PluginFactory
    public static AbstractMvcViewPasswordRewritePolicy createPolicy() {
        return new AbstractMvcViewPasswordRewritePolicy();
    }

    private AbstractMvcViewPasswordRewritePolicy() {
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
            modifiedMessage = originMessage.replaceAll("password=\\S+&", "password=****&");
        }

        return modifiedMessage;
    }
}