<%@ page import="de.triology.cas.services.LegalLinkProducer" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="static org.springframework.web.servlet.support.RequestContextUtils.getWebApplicationContext" %>
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

</div>
<div class="bottom"><%
    ApplicationContext ctx = getWebApplicationContext(request);
    LegalLinkProducer lp = (LegalLinkProducer) ctx.getBean("linkProducer");
    pageContext.setAttribute("tos", lp.getTermsOfServiceLink());
    pageContext.setAttribute("imprint", lp.getImprintLink());
    pageContext.setAttribute("privacy", lp.getPrivacyPolicyLink());
    pageContext.setAttribute("delimiterTos", lp.getTermsOfServiceLinkDelimiter());
    pageContext.setAttribute("delimiterImprint", lp.getImprintLinkDelimiter());
%>
    <c:if test="${not empty tos}">
        <a href="<c:out value="${tos}" escapeXml="false" />" target="_blank">
            <spring:message code="screen.bottom.termsOfService"/>
        </a>
    </c:if>

    <c:out value="${delimiterTos}" escapeXml="false"/>

    <c:if test="${not empty imprint}">
        <a href="<c:out value="${imprint}" escapeXml="false" />" target="_blank">
            <spring:message code="screen.bottom.imprint"/>
        </a>
    </c:if>

    <c:out value="${delimiterImprint}" escapeXml="false"/>

    <c:if test="${not empty privacy}">
        <a href="<c:out value="${privacy}" escapeXml="false" />" target="_blank">
            <spring:message code="screen.bottom.privacyPolicy"/>
        </a>
    </c:if>
</div>

</body>
</html>
