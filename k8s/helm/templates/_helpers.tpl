{{/* Chart basics
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec) starting from
Kubernetes 1.4+.
*/}}
{{- define "cas.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "cas.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "cas.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/* All-in-one labels */}}
{{- define "cas.labels" -}}
app: ces
{{ include "cas.selectorLabels" . }}
helm.sh/chart: {{- printf " %s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- if .Values.extraLabels }}
{{ toYaml .Values.extraLabels }}
{{- end }}
{{- end }}

{{/* Selector labels */}}
{{- define "cas.selectorLabels" -}}
app.kubernetes.io/name: {{ include "cas.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "cas.backupLabels"  -}}
k8s.cloudogu.com/backup-scope: cas
{{- end }}

{{- define "cas.lookupSecretValue" -}}
{{- $root := .root -}}
{{- $secretName := .secretName -}}
{{- $key := .key -}}
{{- $secret := lookup "v1" "Secret" $root.Release.Namespace $secretName -}}
{{- if and $secret $secret.data -}}
{{- with (index $secret.data $key) -}}
{{- . | b64dec -}}
{{- end -}}
{{- end -}}
{{- end -}}
