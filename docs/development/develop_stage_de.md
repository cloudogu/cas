# Entwicklungsmodus

Zum lokalen Testen einiger Dogus kann es notwendig sein, den CAS in den Entwicklungsmodus zu versetzen. 
Das führt dazu, dass alle Applikationen sich über den CAS authentifizieren können, auch wenn sie dort nicht
konfiguriert sind.
Dafür muss mit dem Befehl `etcdctl set /config/_global/stage development` die Stage des EcoSystems auf
`development` gesetzt werden und das Dogu neu gestartet werden.
