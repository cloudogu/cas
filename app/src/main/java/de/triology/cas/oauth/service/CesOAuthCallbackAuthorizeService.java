/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package de.triology.cas.oauth.service;

import de.triology.cas.oauth.CesOAuthConstants;
import org.apereo.cas.services.RegexRegisteredService;

/**
 * An extension of the {@link RegexRegisteredService} that attempts to enforce the
 * correct url syntax for the OAuth callback authorize url. The url must end with
 * {@link CesOAuthConstants#CALLBACK_AUTHORIZE_URL}.
 */
public final class CesOAuthCallbackAuthorizeService extends RegexRegisteredService {

    private static final long serialVersionUID = 1365893114273585648L;

    /**
     * {@inheritDoc}.
     *
     * @throws IllegalArgumentException if the received url does not end with
     *                                  {@link CesOAuthConstants#CALLBACK_AUTHORIZE_URL}
     */
    @Override
    public void setServiceId(final String id) {
        if (!id.endsWith(CesOAuthConstants.CALLBACK_AUTHORIZE_URL)) {
            final String msg = String.format("OAuth callback authorize service id must end with [%s]",
                    CesOAuthConstants.CALLBACK_AUTHORIZE_URL);
            throw new IllegalArgumentException(msg);
        }
        super.setServiceId(id);
    }
}
