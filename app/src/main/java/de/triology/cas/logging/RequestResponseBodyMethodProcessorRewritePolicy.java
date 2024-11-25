package de.triology.cas.logging;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

@Plugin(
        name = "RequestResponseBodyMethodProcessorRewritePolicy",
        category = "Core",
        elementType = "rewritePolicy",
        printObject = true
)
/*
 * Password rewriter for class org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor.
 */
public final class RequestResponseBodyMethodProcessorRewritePolicy extends AbstractCASPasswordRewritePolicy {
    private static final String PARAMETER_PASSWORD_TEXT = "password=";

    @PluginFactory
    public static RequestResponseBodyMethodProcessorRewritePolicy createPolicy() {
        return new RequestResponseBodyMethodProcessorRewritePolicy();
    }

    private RequestResponseBodyMethodProcessorRewritePolicy() {
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
            truncatedMessage = originMessage.replaceAll("password=.*\\]\\}\\]", "password=******]}]");
        }

        return truncatedMessage;
    }
}