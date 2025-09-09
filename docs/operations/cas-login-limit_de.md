# Funktionen

Login-Versuche können beschränkt werden, indem Benutzerkonten nach einer gewissen Anzahl an fehlgeschlagenen
Anmeldeversuchen innerhalb bestimmter Zeitgrenzen temporär gesperrt werden können. Ist ein Konto gesperrt, wird
der Benutzer auf eine entsprechende Seite umgeleitet. Hierbei wird nicht geprüft, ob ein Benutzerkonto existiert
(d. h. auch für nicht existierende Konten wird die Sperre durchgeführt).

<strong>Hinweis: CAS berechnet aus dem failure_threshold und den range_seconds eine Fehler-Rate. Anschließend werden 
<ins>die letzten zwei</ins> fehlerhaften Anmeldungen angeschaut und geprüft, ob die Zeit unter der Fehler-Rate liegt.

Beispiel: Failure_threshold = 500; Range_seconds = 10; Fehler-Rate = 500/10 = 50

=> Eine Fehler-Rate von 50 bedeutet, dass gethrottled wird, wenn zwei fehlerhafte Anmeldungen innerhalb von 0.04 Sekunden
erfolgen: 500/10s -> 50/1s -> 5/0.1s -> 2/0.04s
</strong>

Hierzu werden die folgenden Konfigurations-Parameter des CAS-Moduls genutzt:

* `limit/failure_threshold` Setzt die maximal erlaubte Anzahl an Fehlversuchen pro Benutzerkonto.
  Wird diese Anzahl innerhalb der mit den weiteren Parametern definierten zeitlichen Rahmen
  überschritten, wird die IP-Adresse des Nutzers temporär gesperrt. Wird intern auf eine Fehler-Rate runtergerechnet (siehe Beispiel oben).

  Wird der Wert auf `0` gesetzt, ist das Feature deaktiviert und es findet keine Limitierung statt.
  Bei einem Wert größer Null müssen die anderen Parameter auf sinnvolle Werte gesetzt werden. 
* `limit/range_seconds` Spezifiziert die Zeitdauer, in der die fehlerhaften Anmeldeversuche für das Throtteling ausgewertet werden. Wird intern auf eine Fehler-Rate runtergerechnet (siehe Beispiel oben).

  Die Zeit wird in Sekunden angegeben und muss größer Null sein, wenn das Feature aktiviert ist.
* `limit/lock_time` Gibt an, wie lange die IP-Adresse im Falle einer Überschreitung der Anmeldeversuche gesperrt werden
  soll.

  Die Zeit wird in Sekunden angegeben und muss größer Null sein, wenn das Feature aktiviert ist.

* `limit/stale_removal_interval` Zeit in Sekunden zwischen Hintergrundläufen, die abgelaufene und veraltete Anmeldefehler finden und entfernen (muss eine positive Zahl sein, wirkt sich nur aus, wenn `limit/failure_threshold` > 0 ist; Standardwert ist 60 Sekunden)
