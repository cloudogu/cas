#
# Licensed to Jasig under one or more contributor license
# agreements. See the NOTICE file distributed with this work
# for additional information regarding copyright ownership.
# Jasig licenses this file to you under the Apache License,
# Version 2.0 (the "License"); you may not use this file
# except in compliance with the License.  You may obtain a
# copy of the License at the following location:
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

##
# Services Management Web UI Security
server.name=https://{{ .GlobalConfig.Get "fqdn" }}
server.prefix=${server.name}/cas
cas.securityContext.serviceProperties.service=${server.prefix}/services/j_acegi_cas_security_check
# Names of roles allowed to access the CAS service manager
cas.securityContext.serviceProperties.adminRoles=ROLE_ADMIN
cas.securityContext.casProcessingFilterEntryPoint.loginUrl=${server.prefix}/login
cas.securityContext.ticketValidator.casServerUrlPrefix=${server.prefix}
# IP address or CIDR subnet allowed to access the /status URI of CAS that exposes health check information
cas.securityContext.status.allowedSubnet=127.0.0.1


cas.themeResolver.defaultThemeName=cas-theme-scmm-universe
cas.viewResolver.basename=default_views

##
# Unique CAS node name
# host.name is used to generate unique Service Ticket IDs and SAMLArtifacts.  This is usually set to the specific
# hostname of the machine running the CAS node, but it could be any label so long as it is unique in the cluster.
{{ $domain := (.GlobalConfig.Get "domain") }}
host.name=cas.{{ $domain }}

##
# Database flavors for Hibernate
#
# One of these is needed if you are storing Services or Tickets in an RDBMS via JPA.
#
# database.hibernate.dialect=org.hibernate.dialect.OracleDialect
# database.hibernate.dialect=org.hibernate.dialect.MySQLInnoDBDialect
# database.hibernate.dialect=org.hibernate.dialect.HSQLDialect

##
# CAS Logout Behavior
# WEB-INF/cas-servlet.xml
#
# Specify whether CAS should redirect to the specifyed service parameter on /logout requests
# cas.logout.followServiceRedirects=false

##
# Single Sign-On Session Timeouts
# Defaults sourced from WEB-INF/spring-configuration/ticketExpirationPolices.xml
#
# Maximum session timeout - TGT will expire in maxTimeToLiveInSeconds regardless of usage
# tgt.maxTimeToLiveInSeconds=28800
#
# Idle session timeout -  TGT will expire sooner than maxTimeToLiveInSeconds if no further requests
# for STs occur within timeToKillInSeconds
# tgt.timeToKillInSeconds=7200

##
# Service Ticket Timeout
# Default sourced from WEB-INF/spring-configuration/ticketExpirationPolices.xml
#
# Service Ticket timeout - typically kept short as a control against replay attacks, default is 10s.  You'll want to
# increase this timeout if you are manually testing service ticket creation/validation via tamperdata or similar tools
# st.timeToKillInSeconds=10

##
# Single Logout Out Callbacks
# Default sourced from WEB-INF/spring-configuration/argumentExtractorsConfiguration.xml
#
# To turn off all back channel SLO requests set slo.disabled to true
# slo.callbacks.disabled=false

##
# Service Registry Periodic Reloading Scheduler
# Default sourced from WEB-INF/spring-configuration/applicationContext.xml
#
# Force a startup delay of 2 minutes.
# service.registry.quartz.reloader.startDelay=120000
#
# Reload services every 2 minutes
# service.registry.quartz.reloader.repeatInterval=120000

##
# Log4j
# Default sourced from WEB-INF/spring-configuration/log4jConfiguration.xml:
#
# It is often time helpful to externalize log4j.xml to a system path to preserve settings between upgrades.
# e.g. log4j.config.location=/etc/cas/log4j.xml
# log4j.config.location=classpath:log4j.xml
#
# log4j refresh interval in millis
# log4j.refresh.interval=60000


# ==================================
# == LDAP Authentication settings ==
# ==================================

#========================================
# General properties
#========================================
{{ $ldapProtocol := "" }}{{ $ldapUseStartTLS := "" }}{{ $ldapTrustManager := "" }}
{{- $ldapEncryption := (.Config.Get "ldap/encryption") -}}
{{- if eq $ldapEncryption "startTLS" -}}
    {{- $ldapProtocol = "ldap" -}}
    {{- $ldapUseStartTLS = "true" -}}
    {{- $ldapTrustManager = "org.ldaptive.ssl.DefaultTrustManager" -}}
{{- else if eq $ldapEncryption "startTLSAny" -}}
    {{- $ldapProtocol = "ldap" -}}
    {{- $ldapUseStartTLS = "true" -}}
    {{- $ldapTrustManager = "org.ldaptive.ssl.AllowAnyTrustManager" -}}
{{- else if eq $ldapEncryption "ssl" -}}
    {{- $ldapProtocol = "ldaps" -}}
    {{- $ldapUseStartTLS = "false" -}}
    {{- $ldapTrustManager = "org.ldaptive.ssl.DefaultTrustManager" -}}
{{- else if eq $ldapEncryption "sslAny" -}}
    {{- $ldapProtocol = "ldaps" -}}
    {{- $ldapUseStartTLS = "false" -}}
    {{- $ldapTrustManager = "org.ldaptive.ssl.AllowAnyTrustManager" -}}
{{- else -}}
    {{- $ldapProtocol = "ldap" -}}
    {{- $ldapUseStartTLS = "false" -}}
    {{- $ldapTrustManager = "org.ldaptive.ssl.DefaultTrustManager" -}}
{{- end -}}
ldap.url={{ $ldapProtocol}}://{{ .Config.Get "ldap/host" }}:{{ .Config.Get "ldap/port" }}

# LDAP connection timeout in milliseconds
ldap.connectTimeout=3000

# Whether to use StartTLS (probably needed if not SSL connection)
ldap.useStartTLS={{ $ldapUseStartTLS }}

#========================================
# LDAP connection pool configuration
#========================================
ldap.pool.minSize=3
ldap.pool.maxSize=10
ldap.pool.validateOnCheckout=false
ldap.pool.validatePeriodically=true

# Amount of time in milliseconds to block on pool exhausted condition
# before giving up.
ldap.pool.blockWaitTime=3000

# Frequency of connection validation in seconds
# Only applies if validatePeriodically=true
ldap.pool.validatePeriod=300

# Attempt to prune connections every N seconds
ldap.pool.prunePeriod=300

# Maximum amount of time an idle connection is allowed to be in
# pool before it is liable to be removed/destroyed
ldap.pool.idleTime=600

#========================================
# Authentication
#========================================

{{ $ldapBaseDn := "" }}
{{- $ldapBindDn := "" -}}
{{- $ldapBindPassword := "" -}}
{{- if eq (.Config.Get "ldap/ds_type") "external" -}}
    {{- $ldapBaseDn = (.Config.Get "ldap/base_dn") -}}
    {{- $ldapBindDn = (.Config.Get "ldap/connection_dn") -}}
    {{- $ldapBindPassword = (.Config.GetAndDecrypt "ldap/password") -}}
{{- else -}}
    {{- $ldapBaseDn = printf "%s,%s,%s" "ou=People,o=" $domain ",dc=cloudogu,dc=com" -}}
    {{- $ldapBindDn = (.Config.GetAndDecrypt "sa-ldap/username") -}}
    {{- $ldapBindPassword = (.Config.GetAndDecrypt "sa-ldap/password") -}}
{{- end -}}

# Base DN of users to be authenticated
ldap.authn.baseDn={{ $ldapBaseDn }}

# Manager DN for authenticated searches
ldap.authn.managerDN={{ $ldapBindDn }}

# Manager password for authenticated searches
ldap.authn.managerPassword={{ $ldapBindPassword }}

# Search filter used for configurations that require searching for DNs
#ldap.authn.searchFilter=(&(uid={user})(accountState=active))
{{ $searchFilter := printf "(&%s(%s={user}))" (.Config.Get "ldap/search_filter") (.Config.Get "ldap/attribute_id") }}

ldap.authn.searchFilter=(&{{ .Config.Get "ldap/search_filter" }}({{ .Config.Get "ldap/attribute_id" }}={user}))

# Search filter used for configurations that require searching for DNs
#ldap.authn.format=uid=%s,ou=Users,dc=example,dc=org
ldap.authn.format=uid=%s,ou=Accounts,{{ .Config.Get "ldap/base_dn" }}

#Ldap mapping of result attributes
ldap.authn.attribute.username={{ .Config.Get "ldap/attribute_id" }}
ldap.authn.attribute.cn=cn
ldap.authn.attribute.mail={{ .Config.Get "ldap/attribute_mail" }}
ldap.authn.attribute.givenName=givenName
ldap.authn.attribute.surname=sn
ldap.authn.attribute.displayName=displayName
ldap.authn.attribute.groups={{ .Config.Get "ldap/attribute_group" }}

# member search settings

# settings for ldap group search by member
# base dn for group search e.g.: o=ces.local,dc=cloudogu,dc=com
ldap.authn.groups.baseDn={{ .Config.GetOrDefault "ldap/group_base_dn" ""}}

# search filter for group search {0} will be replaced with the dn of the user
# e.g.: (member={0})
# if this property is empty, group search by member will be skipped
ldap.authn.groups.searchFilter={{ .Config.GetOrDefault "ldap/group_search_filter" ""}}

# name attribute of groups e.g.: cn
ldap.authn.groups.attribute.name={{ .Config.GetOrDefault "ldap/group_attribute_name" ""}}

# use the connection after bind with user dn to fetch attributes
ldap.authn.useUserConnectionToFetchAttributes = {{ .Config.GetOrDefault "ldap/use_user_connection_to_fetch_attributes" "true"}}

ldap.trustManager={{ $ldapTrustManager }}

# set deployment stage
{{ $requireSecure := "true" }}
{{- $stage := (.GlobalConfig.GetOrDefault "stage" "") -}}
{{- if ne $stage "development" -}}
    {{ $stage = "production" }}
{{- else -}}
    {{ $requireSecure = "false" }}
{{- end -}}

stage={{ $stage }}
requireSecure={{ $requireSecure }}

#========================================
# Limit Login Attempts
#========================================
# set login.limit.maxNumber to 0 to disable feature
# time parameters are configured in seconds
login.limit.maxNumber={{ .Config.GetOrDefault "limit/max_number" "0" }}
login.limit.failureStoreTime={{ .Config.GetOrDefault "limit/failure_store_time" "0" }}
login.limit.lockTime={{ .Config.GetOrDefault "limit/lock_time" "0" }}
login.limit.maxAccounts=10000
