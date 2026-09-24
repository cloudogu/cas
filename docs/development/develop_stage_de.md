# Entwicklungsmodus

Zum lokalen Testen einiger Dogus kann es notwendig sein, den CAS in den Entwicklungsmodus zu versetzen. 
Das führt dazu, dass alle Applikationen sich über den CAS authentifizieren können, auch wenn sie dort nicht
konfiguriert sind.

``kubectl edit -n ecosystem configmap global-config``
`````yaml
  data:
    config.yaml: |
      stage: development
  `````