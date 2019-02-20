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

/**
 * Prepares the login form for submission by appending any URI
 * fragment (hash) to the form action in order to propagate it
 * through the re-direct (i.e. store it client side).
 * @param form The login form object.
 * @returns true to allow the form to be submitted.
 */
function prepareSubmit(form) {
    // Extract the fragment from the browser's current location.
    var hash = decodeURIComponent(self.document.location.hash);

    // The fragment value may not contain a leading # symbol
    if (hash && hash.indexOf("#") === -1) {
        hash = "#" + hash;
    }

    // Append the fragment to the current action so that it persists to the redirected URL.
    form.action = form.action + hash;
    return true;
}
