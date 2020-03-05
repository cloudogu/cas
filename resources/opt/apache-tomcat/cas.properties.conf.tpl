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

# Single Sign-On Session Timeouts
# Defaults sourced from WEB-INF/spring-configuration/ticketExpirationPolices.xml
#
# Maximum session timeout - TGT will expire in maxTimeToLiveInSeconds regardless of usage

{{ if .Config.Exists "session_tgt/max_time_to_live_in_seconds" }}
tgt.maxTimeToLiveInSeconds={{ .Config.Get "session_tgt/max_time_to_live_in_seconds"}}
{{ else }}
# default value 24h
tgt.maxTimeToLiveInSeconds=90000
{{ end }}

# Idle session timeout -  TGT will expire sooner than maxTimeToLiveInSeconds if no further requests
# for STs occur within timeToKillInSeconds

{{ if .Config.Exists "session_tgt/time_to_kill_in_seconds" }}
tgt.timeToKillInSeconds={{ .Config.Get "session_tgt/time_to_kill_in_seconds"}}
{{ else }}
# default value 10h
tgt.timeToKillInSeconds=36000
{{ end }}
