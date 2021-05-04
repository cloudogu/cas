<%--

    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License.  You may obtain a
    copy of the License at the following location:

      http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

--%>
<jsp:directive.include file="includes/top.jsp"/>

<script>
    var URL = document.URL;
    var URLsecure = URL.replace("http", "https");

    function toggleForgotPasswordInfo() {
        var x = document.getElementById("forgotPasswordInfo");
        if (x.style.display === "none") {
            x.style.display = "block";
        } else {
            x.style.display = "none";
        }
    }
</script>

<div class="row mid vertical-align">
    <div class="login-container">
        <form:form method="post" id="fm1" cssClass="fm-v clearfix login-form col-lg-8 col-lg-offset-2"
                   commandName="${commandName}" htmlEscape="true"
                   onsubmit="return prepareSubmit(this);">

            <%--Left column - cloudogu logo--%>
            <img src="<c:url value="/themes/ces-theme/dist/images/logo/logo-blue-160px.png" />" alt="Cloudogu Logo"
                 class="spacer-top pull-right cloudogu-icon" aria-hidden="true">

            <%--Right column - Username label & input--%>
            <c:if test="${not empty sessionScope.openIdLocalId}">
                <strong>${sessionScope.openIdLocalId}</strong>
                <input type="hidden" id="username" name="username"
                       value="${sessionScope.openIdLocalId}"/>
            </c:if>
            <c:if test="${empty sessionScope.openIdLocalId}">
                <label for="username" class="username-label">
                    <spring:message code="screen.welcome.label.netid"/>
                </label>
                <form:input cssClass="form-control username-input" cssErrorClass="error" id="username"
                            placeholder="${userPlaceholder}" size="25" tabindex="1"
                            accesskey="${userNameAccessKey}" path="username" autocomplete="false"
                            htmlEscape="true"/>
            </c:if>

            <%--Right column - Password label & input--%>
            <label for="password" class="password-label">
                <spring:message code="screen.welcome.label.password"/>
            </label>
            <form:password cssClass="form-control password-input" cssErrorClass="error" id="password"
                           placeholder="${passwordPlaceholder}" size="25" tabindex="2" path="password"
                           accesskey="${passwordAccessKey}" htmlEscape="true" autocomplete="off"/>
            <spring:message code="screen.password.forgotText" var="passwordForgotText" text=""
                            javaScriptEscape="true"/>

            <%--Right column - Submit button--%>
            <button class="btn btn-primary pull-right submit-button" role="button" name="submit" accesskey="l"
                    value="Login"
                    tabindex="4" type="submit" aria-label="Login">Login
            </button>

            <%--Hidden in any case - necessary inputs for login--%>
            <input type="hidden" name="lt" value="${loginTicket}"/>
            <input type="hidden" name="execution" value="${flowExecutionKey}"/>
            <input type="hidden" name="_eventId" value="submit"/>

            <div class="alert-area">
                <%--tbd msg über un feld--%>
                <spring:message code="screen.welcome.label.netid.accesskey" var="userNameAccessKey"/>
                <spring:message code="screen.welcome.label.netid.placeholder" var="userPlaceholder"/>

                <%--tbd msg über pw feld--%>
                <spring:message code="screen.welcome.label.password.accesskey" var="passwordAccessKey"/>
                <spring:message code="screen.welcome.label.password.placeholder" var="passwordPlaceholder"/>

                <%--password forget--%>
                <c:if test="${not empty passwordForgotText}">
                    <spring:message code="screen.password.forgot" var="passwordForgot" text=""
                                    javaScriptEscape="true"/>
                    <button type="button" id="forgotPassword" class="link-underlined btn btn-primary"
                            aria-label="${passwordForgot}"
                            onclick="toggleForgotPasswordInfo(); return false;">${passwordForgot}</button>
                </c:if>

                <%--tbd msg--%>
                <c:if test="${not pageContext.request.secure}">
                    <div class="alert alert-warning">
                        <spring:message code="screen.welcome.label.httpWarning"/>
                        <a class="link-underlined" href="#"
                           onclick="location.href=URLsecure;">HTTPS</a>.
                    </div>
                </c:if>
                <form:errors path="*" id="msg" role="alert"
                             cssClass="alert alert-danger alert-msg-credentials"
                             element="div"/>
                <div style="display:none;" id="forgotPasswordInfo" class="alert alert-info">
                        ${passwordForgotText}
                </div>
            </div>
        </form:form>
    </div>

    <style>
        .login-container {
            background-color: white;
            min-width: 50rem;
            width: 50%;
            height: 100%;
            margin: auto;
            border-radius: 2rem;
        }

        .login-form {
            display: grid;
            grid-template-columns: 160px 5rem auto 7rem;
            grid-template-rows:  25% 6% 14% 6% 14% 3.5rem 8rem auto;
            height: 100%;
        }

        .username-label {
            grid-row-start: 2;
            grid-row-end: 3;
            grid-column-start: 3;
            grid-column-end: 5;
            color: #1b7daa;
        }

        .username-input {
            grid-row-start: 3;
            grid-row-end: 4;
            grid-column-start: 3;
            grid-column-end: 5;
        }

        .password-label {
            grid-row-start: 4;
            grid-row-end: 5;
            grid-column-start: 3;
            grid-column-end: 5;
            color: #1b7daa;
        }

        .password-input {
            grid-row-start: 5;
            grid-row-end: 6;
            grid-column-start: 3;
            grid-column-end: 5;
        }

        .submit-button {
            grid-row-start: 6;
            grid-row-end: 7;
            grid-column-start: 4;
            grid-column-end: 5;
        }

        .cloudogu-icon {
            grid-row-start: 3;
            grid-row-end: 4;
            grid-column-start: 1;
            grid-column-end: 2;
            width: 160px;
        }

        .alert-area {
            grid-row-start: 7;
            grid-row-end: 8;
            grid-column-start: 1;
            grid-column-end: 5;
            display: flex;
            flex-direction: row;
            align-items: center;
            justify-content: center;
        }
    </style>
</div>

<jsp:directive.include file="includes/bottom.jsp"/>
