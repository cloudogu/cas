<?xml version="1.0" encoding="UTF-8" ?>
<!--

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

-->
<!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">

<log4j:configuration debug="false" xmlns:log4j="http://jakarta.apache.org/log4j/">
    <!--
      This default ConsoleAppender is used to log all NON perf4j messages
      to System.out
    -->
    <appender name="console" class="org.apache.log4j.ConsoleAppender">
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="%d{dd-MMM-yyyy HH:mm:ss.SSS} %p [%c] - %m%n"/>
        </layout>
    </appender>

    <appender name="cas" class="org.apache.log4j.RollingFileAppender">
        <param name="File" value="logs/cas.log" />
        <param name="MaxFileSize" value="512KB" />
        <param name="MaxBackupIndex" value="3" />
        <layout class="org.apache.log4j.PatternLayout">
            <param name="ConversionPattern" value="%d{dd-MMM-yyyy HH:mm:ss.SSS} %p [%c] - %m%n"/>
        </layout>
    </appender>

    <!-- Perf4J appenders -->
    <!--
       This AsyncCoalescingStatisticsAppender groups StopWatch log messages
       into GroupedTimingStatistics messages which it sends on the
       file appender defined below
    -->
    <appender name="CoalescingStatistics" class="org.perf4j.log4j.AsyncCoalescingStatisticsAppender">
        <param name="TimeSlice" value="60000"/>
        <appender-ref ref="console"/>
        <appender-ref ref="graphExecutionTimes"/>
        <appender-ref ref="graphExecutionTPS"/>
    </appender>

    <appender name="graphExecutionTimes" class="org.perf4j.log4j.GraphingStatisticsAppender">
        <!-- Possible GraphTypes are Mean, Min, Max, StdDev, Count and TPS -->
        <param name="GraphType" value="Mean"/>
        <!-- The tags of the timed execution blocks to graph are specified here -->
        <param name="TagNamesToGraph" value="DESTROY_TICKET_GRANTING_TICKET,GRANT_SERVICE_TICKET,GRANT_PROXY_GRANTING_TICKET,VALIDATE_SERVICE_TICKET,CREATE_TICKET_GRANTING_TICKET,AUTHENTICATE" />
    </appender>

    <appender name="graphExecutionTPS" class="org.perf4j.log4j.GraphingStatisticsAppender">
        <param name="GraphType" value="TPS" />
        <param name="TagNamesToGraph" value="DESTROY_TICKET_GRANTING_TICKET,GRANT_SERVICE_TICKET,GRANT_PROXY_GRANTING_TICKET,VALIDATE_SERVICE_TICKET,CREATE_TICKET_GRANTING_TICKET,AUTHENTICATE" />
    </appender>

    <!-- Loggers -->
    {{$loglevel := .Config.GetOrDefault "logging/root" "WARN"}}
    {{if eq $loglevel "INFO" "DEBUG"}}
    <!--
      The Perf4J logger. Note that org.perf4j.TimingLogger is the value of the
      org.perf4j.StopWatch.DEFAULT_LOGGER_NAME constant. Also, note that
      additivity is set to false, which is usually what is desired - this means
      that timing statements will only be sent to this logger and NOT to
      upstream loggers.
    -->
    <logger name="org.perf4j.TimingLogger" additivity="false">
        <level value="{{ $loglevel }}" />
        <appender-ref ref="CoalescingStatistics" />
    </logger>
    {{end}}
    <!--
        WARNING: Setting the org.springframework logger to DEBUG displays debug information about
        the request parameter values being bound to the command objects.  This could expose your
        password in the log file.  If you are sharing your log files, it is recommend you selectively
        apply DEBUG level logging on a an org.springframework.* package level (i.e. org.springframework.dao)
    -->
    <logger name="org.springframework">
        <level value='{{ .Config.GetOrDefault "logging/root" "WARN"}}' />
        <appender-ref ref="console" />
    </logger>

    <logger name="org.springframework.webflow">
        <level value='{{ .Config.GetOrDefault "logging/root" "WARN"}}' />
        <appender-ref ref="console" />
    </logger>

    <!--
        At log level debug, the password would be output in plain text.
        Therefore, the following class is never logged on debug.
    -->
    {{if $loglevel eq "DEBUG"}}
    <logger name="org.springframework.binding.mapping.impl.DefaultMapping">
        <level value='INFO' />
        <appender-ref ref="console" />
    </logger>
    {{end}}

    <logger name="de.triology" additivity="true">
        <level value='{{ .Config.GetOrDefault "logging/root" "WARN"}}' />
        <appender-ref ref="console" />
    </logger>

    <logger name="org.jasig" additivity="true">
        <level value='{{ .Config.GetOrDefault "logging/root" "WARN"}}' />
        <appender-ref ref="console" />
    </logger>

    <logger name="com.github.inspektr.audit.support.Slf4jLoggingAuditTrailManager">
        <level value='{{ .Config.GetOrDefault "logging/root" "WARN"}}' />
        <appender-ref ref="console" />
    </logger>

    <!--
        Suppress missing i18n key messages e.g.:
        The code [xxx] cannot be found in the language bundle for the locale [en_US]
    -->
    <logger name="org.jasig.cas.web.view.CasReloadableMessageBundle">
        <level value='{{ .Config.GetOrDefault "logging/translation_messages" "ERROR"}}' />
        <appender-ref ref="console" />
    </logger>

    <!--
        WARNING: Setting the flow package to DEBUG will display
        the parameters posted to the login servlet including
        cleartext authentication credentials
    -->
    <logger name="org.jasig.cas.web.flow" additivity="true">
        <level value='{{ .Config.GetOrDefault "logging/root" "WARN"}}' />
        <appender-ref ref="console" />
    </logger>

    <!--
      The root logger sends all log statements EXCEPT those sent to the perf4j
      logger to System.out.
    -->
    <root>
        <level value='{{ .Config.GetOrDefault "logging/root" "WARN"}}' />
        <appender-ref ref="console" />
    </root>
</log4j:configuration>


