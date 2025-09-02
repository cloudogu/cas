<?xml version="1.0" encoding="UTF-8" ?>
<!-- Specify the refresh internal in seconds. -->
<Configuration monitorInterval="5">
    <Properties>
        <Property name="baseDir">logs</Property>
        <Property name="ces.log.level">{{ .Config.GetOrDefault "logging/root" "warn"}}</Property>
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
            <DefaultRolloverStrategy max="7">
                <Delete basePath="${baseDir}" maxDepth="1">
                    <IfFileName glob="cas-*.log" />
                    <IfLastModified age="7d" />
                </Delete>
            </DefaultRolloverStrategy>
        </RollingFile>

        <RollingFile name="auditlogfile" fileName="${baseDir}/cas_audit.log" append="true"
                     filePattern="${baseDir}/cas_audit-%d{yyyy-MM-dd-HH}-%i.log">
            <PatternLayout pattern="%d %p [%c] - %m%n"/>
            <Policies>
                <OnStartupTriggeringPolicy />
                <SizeBasedTriggeringPolicy size="10 MB"/>
                <TimeBasedTriggeringPolicy />
            </Policies>
            <DefaultRolloverStrategy max="7">
                <Delete basePath="${baseDir}" maxDepth="1">
                    <IfFileName glob="cas_audit-*.log" />
                    <IfLastModified age="7d" />
                </Delete>
            </DefaultRolloverStrategy>
        </RollingFile>

        <CasAppender name="casAudit">
            <AppenderRef ref="auditlogfile" />
        </CasAppender>
        <!-- Currently, appender casFile is not used as application runs in container -->
        <CasAppender name="casFile">
            <AppenderRef ref="file" />
        </CasAppender>
        <CasAppender name="casConsole">
            <AppenderRef ref="console" />
        </CasAppender>

        <Rewrite name="R1-LoggingHandler">
            <AppenderRef ref="casConsole"/>
            <LoggingHandlerPasswordRewritePolicy/>
        </Rewrite>

        <Rewrite name="R2-DefaultMapping">
            <AppenderRef ref="R1-LoggingHandler"/>
            <DefaultMappingPasswordRewritePolicy/>
        </Rewrite>

        <Rewrite name="R3-AbstractMvcView">
            <AppenderRef ref="R2-DefaultMapping"/>
            <AbstractMvcViewPasswordRewritePolicy/>
        </Rewrite>

        <Rewrite name="R4-StringConverter">
            <AppenderRef ref="R3-AbstractMvcView"/>
            <StringConverterPasswordRewritePolicy/>
        </Rewrite>

        <Rewrite name="R5-MisspelledPassword">
            <AppenderRef ref="R4-StringConverter"/>
            <MisspelledPasswordRewritePolicy/>
        </Rewrite>

        <Rewrite name="R6-DelegatedIdPProducer">
            <AppenderRef ref="R5-MisspelledPassword"/>
            <DefaultDelegatedClientIdentityProviderConfigurationProducerRewritePolicy/>
        </Rewrite>

        <Rewrite name="SanitizePasswords">
            <AppenderRef ref="R6-DelegatedIdPProducer"/>
            <RequestResponseBodyMethodProcessorRewritePolicy/>
        </Rewrite>
    </Appenders>
    <Loggers>
        <!-- If adding a Logger with level set higher than ${sys:ces.log.level}, make category as selective as possible -->
        <!-- Loggers inherit appenders from Root Logger unless additivity is false -->
        <AsyncLogger name="org.apereo" level="${sys:ces.log.level}" includeLocation="true"/>

        <AsyncLogger name="org.apache" level="${sys:ces.log.level}" />

        <AsyncLogger name="org.springframework" level="${sys:ces.log.level}" includeLocation="true" />
        <AsyncLogger name="org.springframework.webflow" level="${sys:ces.log.level}" includeLocation="true" />

        <AsyncLogger name="de.triology" level="${sys:ces.log.level}" includeLocation="true"/>

        <AsyncLogger name="org.pac4j" level="${sys:ces.log.level}" includeLocation="true"/>

        <!-- prevent log spamming of unusable log entries -->
        <AsyncLogger name="org.apache.catalina" level="error" includeLocation="true"/>
        <AsyncLogger name="org.springframework.jndi" level="error" includeLocation="true"/>

        <!-- Log audit to all root appenders, and also to audit log (additivity is not false) -->
        <AsyncLogger name="org.apereo.inspektr.audit.support" level="${sys:ces.log.level}" includeLocation="true" additivity="false" >
            <AppenderRef ref="casAudit"/>
        </AsyncLogger>

        <!-- Rewrite messages with passwords in plain text - The following classes would otherwise output passwords in plain text at log level debug.-->
        <AsyncLogger name="org.springframework.binding.mapping.impl.DefaultMapping" level="${sys:ces.log.level}" includeLocation="true" additivity="false">
            <AppenderRef ref="R2-DefaultMapping"/>
        </AsyncLogger>
        <AsyncLogger name="org.springframework.webflow.mvc.view.AbstractMvcView" level="${sys:ces.log.level}" includeLocation="true" additivity="false">
            <AppenderRef ref="R3-AbstractMvcView"/>
        </AsyncLogger>
        <AsyncLogger name="org.apache.commons.beanutils.converters.StringConverter" level="${sys:ces.log.level}" includeLocation="true" additivity="false">
            <AppenderRef ref="R4-StringConverter"/>
        </AsyncLogger>
        <AsyncLogger name="io.netty.handler.logging.LoggingHandler" level="${sys:ces.log.level}" includeLocation="true" additivity="false">
            <AppenderRef ref="R1-LoggingHandler"/>
        </AsyncLogger>
        <AsyncLogger name="org.apereo.cas.web" level="${sys:ces.log.level}" includeLocation="true" additivity="false">
            <AppenderRef ref="R5-MisspelledPassword"/>
        </AsyncLogger>
        <AsyncLogger name="org.apereo.cas.web.flow.DefaultDelegatedClientIdentityProviderConfigurationProducer" level="${sys:ces.log.level}" includeLocation="true" additivity="false">
            <AppenderRef ref="R6-DelegatedIdPProducer"/>
        </AsyncLogger>
        <AsyncLogger name="org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor" level="${sys:ces.log.level}" includeLocation="true" additivity="false">
            <AppenderRef ref="SanitizePasswords"/>
        </AsyncLogger>

        <!-- All Loggers inherit appenders specified here, unless additivity="false" on the Logger -->
        <AsyncRoot level="${sys:ces.log.level}">
            <!--
                 For deployment to an application server running as service,
                 delete the casConsole appender below
            -->
            <AppenderRef ref="casConsole"/>
        </AsyncRoot>
    </Loggers>
</Configuration>
