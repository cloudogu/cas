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
package de.triology.cas.oauth;

/**
 * This class has the main constants for the OAuth implementation.
 *
 * @author Jerome Leleu
 * @since 3.5.0
 */
public interface CesOAuthConstants {

    String REDIRECT_URI = "redirect_uri";

    String CLIENT_ID = "client_id";

    String CLIENT_SECRET = "client_secret";

    String CODE = "code";

    String SERVICE = "service";

    String TICKET = "ticket";

    String STATE = "state";

    String ACCESS_TOKEN = "access_token";

    String OAUTH20_CALLBACKURL = "oauth20_callbackUrl";

    String OAUTH20_SERVICE_NAME = "oauth20_service_name";

    String OAUTH20_STATE = "oauth20_state";

    String MISSING_ACCESS_TOKEN = "missing_accessToken";

    String EXPIRED_ACCESS_TOKEN = "expired_accessToken";

    String CONFIRM_VIEW = "oauthConfirmView";

    String ERROR_VIEW = "casServiceErrorView";

    String INVALID_REQUEST = "invalid_request";

    String INVALID_GRANT = "invalid_grant";

    String AUTHORIZE_URL = "authorize";

    String CALLBACK_AUTHORIZE_URL = "callbackAuthorize";

    String ACCESS_TOKEN_URL = "accessToken";

    String PROFILE_URL = "profile";

    String CONTENT_TYPE_JSON = "application/json";

    String TOKEN_TYPE = "token_type";

    String TOKEN_EXPIRES = "expires_in";

    String EXPIRES = "expires";

    String TOKEN_TYPE_VALUE = "Bearer";

    String OAUTH_AUTHORIZATION_SERVICE_ID = "https://oauthdebugger.com/debug";
}
