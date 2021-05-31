<?xml version="1.0" encoding="UTF-8" ?>
<!-- Specify the refresh internal in seconds. -->
<Configuration monitorInterval="5" packages="org.apereo.cas.logging,de.triology.cas.logging">
    <Properties>
        <Property name="baseDir">logs</Property>
        <Property name="ces.log.level">{{ .Config.GetOrDefault "logging/root" "warn"}}</Property>
        <Property name="ces.translation.messages.log.level">{{ .Config.GetOrDefault "logging/translation_messages" "error"}}</Property>
    </Properties>
    <Appenders>
        <Console name="console" target="SYSTEM_OUT">
            <PatternLayout pattern="%highlight{%d %p [%c] - &lt;%m&gt;}%n"/>
        </Console>
        <RollingFile name="file" fileName="${baseDir}/cas.log" append="true"
                     filePattern="${baseDir}/cas-%d{yyyy-MM-dd-HH}-%i.log">
            <PatternLayout pattern="%d %p [%c] - &lt;%m&gt;%n"/>
            <Policies>
                <OnStartupTriggeringPolicy />
                <SizeBasedTriggeringPolicy size="10 MB"/>
                <TimeBasedTriggeringPolicy />
            </Policies>
        </RollingFile>
        <RollingFile name="auditlogfile" fileName="${baseDir}/cas_audit.log" append="true"
                     filePattern="${baseDir}/cas_audit-%d{yyyy-MM-dd-HH}-%i.log">
            <PatternLayout pattern="%d %p [%c] - %m%n"/>
            <Policies>
                <OnStartupTriggeringPolicy />
                <SizeBasedTriggeringPolicy size="10 MB"/>
                <TimeBasedTriggeringPolicy />
            </Policies>
        </RollingFile>

        <CasAppender name="casAudit">
            <AppenderRef ref="auditlogfile" />
        </CasAppender>
        <CasAppender name="casFile">
            <AppenderRef ref="file" />
        </CasAppender>
        <CasAppender name="casConsole">
            <AppenderRef ref="console" />
        </CasAppender>

        <Rewrite name="rewrite" >
            <PasswordRewritePolicy />
            <AppenderRef ref="casConsole" />
        </Rewrite>
    </Appenders>
    <Loggers>
        <!-- If adding a Logger with level set higher than ${sys:ces.log.level}, make category as selective as possible -->
        <!-- Loggers inherit appenders from Root Logger unless additivity is false -->
        <AsyncLogger name="org.apereo" level="${sys:ces.log.level}" includeLocation="true"/>
        <AsyncLogger name="org.apereo.cas.web.view.CasReloadableMessageBundle" level="${sys:ces.translation.messages.log.level}" includeLocation="true"/>

        <AsyncLogger name="org.apache" level="${sys:ces.log.level}" />

        <AsyncLogger name="org.springframework" level="${sys:ces.log.level}" includeLocation="true" />
        <AsyncLogger name="org.springframework.webflow" level="${sys:ces.log.level}" includeLocation="true" />
        <AsyncLogger name="org.springframework.binding.mapping.impl.DefaultMapping" level="${sys:ces.log.level}" includeLocation="true" additivity="false">
            <AppenderRef ref="rewrite"/>
        </AsyncLogger>

        <AsyncLogger name="de.triology" level="${sys:ces.log.level}" includeLocation="true"/>

        <AsyncLogger name="org.pac4j" level="${sys:ces.log.level}" includeLocation="true"/>

        <!-- Log audit to all root appenders, and also to audit log (additivity is not false) -->
        <AsyncLogger name="org.apereo.inspektr.audit.support" level="${sys:ces.log.level}" includeLocation="true" >
            <AppenderRef ref="casAudit"/>
        </AsyncLogger>

        <!-- All Loggers inherit appenders specified here, unless additivity="false" on the Logger -->
        <AsyncRoot level="${sys:ces.log.level}">
            <AppenderRef ref="casFile"/>
            <!--
                 For deployment to an application server running as service,
                 delete the casConsole appender below
            -->
            <AppenderRef ref="casConsole"/>
        </AsyncRoot>
    </Loggers>
</Configuration>
