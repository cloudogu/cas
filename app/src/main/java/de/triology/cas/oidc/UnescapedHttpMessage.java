package de.triology.cas.oidc;

import de.triology.cas.oidc.beans.CesOidcSingleLogoutServiceMessageHandler;
import org.apereo.cas.util.EncodingUtils;
import org.apereo.cas.util.http.HttpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

public class UnescapedHttpMessage extends HttpMessage {
    protected static final Logger LOG = LoggerFactory.getLogger(UnescapedHttpMessage.class);

    public UnescapedHttpMessage(URL url, String message) {
        super(url, message);
    }

    /**
     * Encodes the message in UTF-8 format in preparation to send.
     *
     * @param message Message to format and encode
     * @return The encoded message.
     */
    protected String formatOutputMessageInternal(final String message) {
        return message;
    }
}
