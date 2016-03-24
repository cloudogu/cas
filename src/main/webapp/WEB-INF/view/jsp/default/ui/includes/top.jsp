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
<html lang="en">
  <head>
    <meta charset="utf-8">
    <title>CAS &#8211; Central Authentication Service</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <link type="text/css" rel="stylesheet" href="<c:url value="/themes/ces-theme/dist/css/ces.css" />" media="screen" />

    <!-- favicons -->
    <link rel="icon" type="image/png" href="<c:url value="/themes/ces-theme/dist/images/favicon/favicon-64px.png" />" sizes="64x64"/>
    <link rel="icon" type="image/png" href="<c:url value="/themes/ces-theme/dist/images/favicon/favicon-32px.png" />" sizes="64x64"/>
    <link rel="icon" type="image/png" href="<c:url value="/themes/ces-theme/dist/images/favicon/favicon-16px.png" />" sizes="64x64"/>

    <!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
    <script src="../bower_components/html5shiv/dist/html5shiv.js"></script>
    <script src="../bower_components/respond/dest/respond.min.js"></script>
    <![endif]-->
    <style>
      head,
      body {
        height: 100%;
        width: 100%;
      }
    </style>
  </head>
  <body class="bg-primary">
    <div class="container vertical-center">
