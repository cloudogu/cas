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
<!DOCTYPE html>
<%@ page session="true" %>
<%@ page pageEncoding="UTF-8" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<spring:theme code="mobile.custom.css.file" var="mobileCss" text="" />
<html xmlns="http://www.w3.org/1999/xhtml" lang="en">
	<head>
	    <title>CAS &#8211; Central Authentication Service</title>

	
        <!--<c:if test="${not empty requestScope['isMobile'] and not empty mobileCss}">

		<!--<meta name="viewport" content="width=device-width, initial-scale=1, minimum-scale=1, maximum-scale=1">
             <meta name="apple-mobile-web-app-capable" content="yes" />
             <meta name="apple-mobile-web-app-status-bar-style" content="black" />

             <link type="text/css" rel="stylesheet" href="<c:url value="/css/fss-mobile-${requestScope['browserType']}-layout.css" />" />
             <link type="text/css" rel="stylesheet" href="${mobileCss}" />
        </c:if>-->
		<spring:theme code="standard.custom.css.file" var="customCssFile" />
		<meta name="viewport" content="width=device-width; initial-scale=1.0; maximum-scale=2.0; user-scalable=1;" />
		<!--<link type="text/css" rel="stylesheet" media="screen" href="<c:url value="/css/fss-framework-1.1.2.css" />" />-->
        <link type="text/css" rel="stylesheet" href="<c:url value="${customCssFile}" />" />
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
	    <link type="image/x-icon" href="/favicon.ico" rel="shortcut icon">	
		<meta charset="utf-8" /> <!-- Legt die Zeichenkodierung fest -->
				
	</head>