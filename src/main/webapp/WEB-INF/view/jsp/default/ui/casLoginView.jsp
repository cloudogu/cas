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
<jsp:directive.include file="includes/top.jsp" />

    <script>
        var URL=document.URL;
        var URLsecure=URL.replace("http","https");
    </script>
    
    <div class="row mid vertical-align">
      <div class="col-md-3 col-md-offset-2 logo">
        <img src="<c:url value="/themes/ces-theme/dist/images/logo/logo-white-160px.png" />" alt="" class="spacer-top pull-right">
      </div>
      <div class="col-md-3">

    <form:form method="post" id="fm1" cssClass="fm-v clearfix" commandName="${commandName}" htmlEscape="true">
        
        <div id="login">

            <c:if test="${not empty sessionScope.openIdLocalId}">
            <strong>${sessionScope.openIdLocalId}</strong>
            <input type="hidden" id="username" name="username" value="${sessionScope.openIdLocalId}" />
            </c:if>
            
            <c:if test="${empty sessionScope.openIdLocalId}">
            <div class="form-group">
                            
                <label for="username">
                    <spring:message code="screen.welcome.label.netid" />
                </label>
                <spring:message code="screen.welcome.label.netid.accesskey" var="userNameAccessKey" />
                <spring:message code="screen.welcome.label.netid.placeholder" var="userPlaceholder"/>
                <form:input cssClass="form-control" cssErrorClass="error" id="username" placeholder="${userPlaceholder}" size="25" tabindex="1" accesskey="${userNameAccessKey}" path="username" autocomplete="false" htmlEscape="true" />
            </div>
            </c:if>

            <div class="form-group">
                <label for="password">
                    <spring:message code="screen.welcome.label.password" />
                </label>
                <spring:message code="screen.welcome.label.password.accesskey" var="passwordAccessKey" />
                <spring:message code="screen.welcome.label.password.placeholder" var="passwordPlaceholder"/>
                <form:password cssClass="form-control" cssErrorClass="error" id="password" placeholder= "${passwordPlaceholder}" size="25" tabindex="2" path="password"  accesskey="${passwordAccessKey}" htmlEscape="true" autocomplete="off" />
            </div>
            
            <input type="hidden" name="lt" value="${loginTicket}" />
            <input type="hidden" name="execution" value="${flowExecutionKey}" />
            <input type="hidden" name="_eventId" value="submit" />
            
            
           
            
            <c:if test="${not pageContext.request.secure}">
            <div class="alert alert-warning warning-msg-https">
              You are currently accessing CAS over a non-secure connection. Single Sign On WILL NOT WORK.  In order to have single sign on work, you MUST log in over <a class="link-underlined" href="#" onclick="location.href=URLsecure;">HTTPS</a>.
            </div>
            </c:if>
            
            <input class="btn btn-primary pull-right" name="submit" accesskey="l" value="Login" tabindex="4" type="submit" />
            <form:errors path="*" id="msg" cssClass="alert alert-danger alert-msg-credentials" element="div" />
        </div>
    </form:form>
 
    </div>
  </div>

<jsp:directive.include file="includes/bottom.jsp" />
