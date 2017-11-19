/* Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theseventhsense.oauth2.sasl;

import java.security.Provider;
import java.security.Security;
import java.util.logging.Logger;


/**
 * Performs OAuth2 authentication.
 * <p>
 * <p>Before using this class, you must call {@code initialize} to install the
 * OAuth2 SASL provider.
 */
public class OAuth2Authenticator {
    private static final Logger logger =
            Logger.getLogger(OAuth2Authenticator.class.getName());

    /**
     * Installs the OAuth2 SASL provider. This must be called exactly once before
     * calling other methods on this class.
     */
    public static void initialize() {
        Security.addProvider(new OAuth2Provider());
    }

    public static final class OAuth2Provider extends Provider {
        private static final long serialVersionUID = 1L;

        public OAuth2Provider() {
            super("Google OAuth2 Provider", 1.0,
                    "Provides the XOAUTH2 SASL Mechanism");
            put("SaslClientFactory.XOAUTH2",
                    "com.theseventhsense.oauth2.sasl.OAuth2SaslClientFactory");
        }
    }
}
