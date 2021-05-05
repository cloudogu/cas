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
        <form:form method="post" id="fm1"
                   cssClass="fm-v clearfix login-form col-lg-8 col-lg-offset-2 col-md-10 col-md-offset-1"
                   commandName="${commandName}" htmlEscape="true"
                   onsubmit="return prepareSubmit(this);">

            <%--Left column - cloudogu logo--%>
            <img src="<c:url value="/themes/ces-theme/dist/images/logo/logo-blue-160px.png" />" alt="Cloudogu Logo"
                 class="spacer-top pull-right cloudogu-icon" aria-hidden="true">

            <%--Right column - Username label & input--%>
            <spring:message code="screen.welcome.label.netid.accesskey" var="userNameAccessKey"/>
            <spring:message code="screen.welcome.label.netid.placeholder" var="userPlaceholder"/>
            <c:if test="${not empty sessionScope.openIdLocalId}">
                <strong>${sessionScope.openIdLocalId}</strong>
                <input type="hidden" id="username" name="username"
                       value="${sessionScope.openIdLocalId}"/>
            </c:if>
            <c:if test="${empty sessionScope.openIdLocalId}">
                <label for="username" class="username-label right">
                    <spring:message code="screen.welcome.label.netid"/>
                </label>
                <form:input cssClass="form-control username-input right" cssErrorClass="error" id="username"
                            placeholder="${userPlaceholder}" size="25" tabindex="1"
                            accesskey="${userNameAccessKey}" path="username" autocomplete="false"
                            htmlEscape="true"/>
            </c:if>

            <%--Right column - Password label & input--%>
            <spring:message code="screen.welcome.label.password.accesskey" var="passwordAccessKey"/>
            <spring:message code="screen.welcome.label.password.placeholder" var="passwordPlaceholder"/>
            <label for="password" class="password-label right">
                <spring:message code="screen.welcome.label.password"/>
            </label>
            <form:password cssClass="form-control password-input right" cssErrorClass="error" id="password"
                           placeholder="${passwordPlaceholder}" size="25" tabindex="2" path="password"
                           accesskey="${passwordAccessKey}" htmlEscape="true" autocomplete="off"/>
            <spring:message code="screen.password.forgotText" var="passwordForgotText" text=""
                            javaScriptEscape="true"/>

            <%--password forget--%>
            <c:if test="${not empty passwordForgotText}">
                <spring:message code="screen.password.forgot" var="passwordForgot" text=""
                                javaScriptEscape="true"/>
                <button type="button" id="forgotPassword" class="link-underlined btn btn-primary right"
                        aria-label="${passwordForgot}"
                        onclick="toggleForgotPasswordInfo(); return false;">${passwordForgot}</button>
            </c:if>

            <%--Right column - Submit button--%>
            <button class="btn btn-primary pull-right submit-button" role="button" name="submit" accesskey="l"
                    value="Login"
                    tabindex="4" type="submit" aria-label="Login">Login
            </button>

            <%--Hidden in any case - necessary inputs for login--%>
            <input type="hidden" name="lt" value="${loginTicket}"/>
            <input type="hidden" name="execution" value="${flowExecutionKey}"/>
            <input type="hidden" name="_eventId" value="submit"/>

            <div class="forgot-password-message-area">
                <div style="display:none;" id="forgotPasswordInfo" class="alert alert-info">
                        ${passwordForgotText}
                </div>
            </div>

            <div class="alert-area">
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
            </div>
        </form:form>
    </div>

    <style>
        .login-container {
            background-color: white;
            height: 100%;
            margin: auto;
            border-radius: 2rem;
        }

        .right {
            grid-column-start: 3;
            grid-column-end: 5;
        }

        .username-label {
            grid-row-start: 2;
            grid-row-end: 3;
            color: #1b7daa;
        }

        .username-input {
            grid-row-start: 3;
            grid-row-end: 4;
        }

        .password-label {
            grid-row-start: 4;
            grid-row-end: 5;
            color: #1b7daa;
        }

        .password-input {
            grid-row-start: 5;
            grid-row-end: 6;
        }

        .submit-button {
            grid-row-start: 6;
            grid-row-end: 7;
            grid-column-start: 4;
            grid-column-end: 5;
        }

        .cloudogu-icon {
            grid-row-start: 2;
            grid-row-end: 4;
            grid-column-start: 1;
            grid-column-end: 2;
            width: 160px;
        }

        #forgotPassword {
            grid-row-start: 6;
            grid-row-end: 7;
            grid-column-start: 3;
            grid-column-end: 4;
            width: 16rem;
            margin: 0;
        }

        .forgot-password-message-area, .alert-area{
            display: flex;
            flex-direction: row;
            align-items: center;
            justify-content: center;
            padding: 10px;
        }

        .forgot-password-message-area > *, .alert-area > *{
            margin-bottom: 0;
            margin-top: 0;
            height: 100%;
        }

        .forgot-password-message-area {
            grid-row-start: 7;
            grid-row-end: 8;
            grid-column-start: 1;
            grid-column-end: 5;

        }

        .alert-area {
            grid-row-start: 8;
            grid-row-end: 9;
            grid-column-start: 1;
            grid-column-end: 5;
        }

        @media (min-width: 961px) {
            .login-container {
                width: 50%;
                min-width: 650px;
            }

            .login-form {
                display: grid;
                grid-template-columns: 160px 30px auto 70px;
                grid-template-rows:  114px 18px 49px 18px 49px 35px 55px 55px auto;
                height: 100%;
            }
        }

        @media (max-width: 960px) and (min-width: 461px) {
            .login-container {
                width: 70%;
                min-width: 461px;
            }

            .login-form {
                display: grid;
                grid-template-columns: 160px 30px auto 70px;
                grid-template-rows:  114px 18px 49px 18px 49px 35px 55px 55px auto;
                height: 100%;
            }
        }

        @media (max-width: 460px) {
            .login-container {
                width: 100%;
                min-width: 360px;
            }

            .login-form {
                display: grid;
                grid-template-columns: 160px 5px auto 70px;
                grid-template-rows:  114px 18px 49px 18px 49px 35px 55px 55px auto;
                height: 100%;
            }
        }
    </style>
</div>

<jsp:directive.include file="includes/bottom.jsp"/>
