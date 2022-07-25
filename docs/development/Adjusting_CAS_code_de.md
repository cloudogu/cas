# Anpassung des CAS-Codes

Es gibt Situationen, da möchten wir das Verhalten vom CAS modifizieren und dazu muss der Code vom CAS angepasst bzw.
überschrieben werden. Ein Großteil des CAS-Verhaltens lässt sich zwar über Propertys steuern, jedoch nicht alles. In
diesem Artikel soll anhand von praktischen Beispielen beschrieben werden, wie das Verhalten vom CAS durch Anpassung des
Quellcodes modifiziert werden kann.

CAS selbst ist Open Source. Der Quellcode vom CAS ist in Github zu finden: https://github.com/apereo/cas/ . Die
Anpassungen erfolgen ausschließlich in unserem CAS-Repository - es werden keine Änderungen am Original-CAS-Code
vorgenommen. Zudem meint "überschreiben" hier, das Erweitern von Java-Klassen, in denen dann einzelne Methoden
überschrieben werden, sowie die Anpassung von Texten durch Verwendung bestimmter Propertys.

Die anzupassenden und zu überschreibenden Stellen müssen eigenständig im CAS-Code gefunden werden. FÜr die Suche können
z.B. Fehlermeldungen, Anzeigetexte oder Log-Ausgaben hilfreich sein.

## Beispiel 1: Keine Fehlermeldung beim Passwort-Zurücksetzen, wenn Nutzername nicht existiert

### Anwendungsfall

Im 1. Beispiel geht es um die Anpassung einer Fehlermeldung bei der Passwort-Zurücksetzen-Funktion. Diese Funktionalität
funktioniert so, dass ein Nutzer seinen Nutzernamen eingeben muss und anschließend eine E-Mail mit einem Link zum Ändern
seines Passworts zugeschickt bekommt.

Das Verhalten vom CAS hierbei ist, dass eine Fehlermeldung angezeigt wird, wenn der eingegebene Nutzername im System
nicht existiert. Dies ist zwar für den Nutzer hilfreich - z.B. falls dieser sich vertippt hat - birgt aber gleichzeitig
die Gefahr, dass existierende und nicht existierende Nutzernamen im System gezielt ermittelt werden können.

Das Verhalten ist so angepasst worden, dass keine Fehlermeldung erscheint, wenn der Nutzername im System nicht
existiert. Der Hinweistext, dass eine E-Mail verschickt worden ist, ist zudem um einen Hinweis erweitert worden. In
diesem Hinweis steht, dass die E-Mail nur verschickt worden ist, wenn der Nutzer auch tatsächlich im System existiert.

### Angepasster Quellcode

Die Änderungen sind im Rahmen von Issue #161 erfolgt: https://github.com/cloudogu/cas/issues/161 .

#### Hinzufügen erforderlicher Dependencies

Um den CAS-Code überschreiben zu können, ist es für die Kompilierung erforderlich, dass alle erforderlichen
Abhängigkeiten eingebunden werden. Abhängigkeiten werden in der Daten `build.gradle` eingebunden. Für dieses Beispiel
mussten die CAS-Dependencies `cas-server-core-notification`, `cas-server-core-notification`
, `cas-server-core-notification`, `cas-server-support-pm-core` und `cas-server-support-pm-webflow` eingebunden werden.

Diese Abhängigkeiten mussten eingebunden werden, da Klassen aus diesen Abhängigkeiten im überschriebenen Quellcode
verwendet werden.

#### Anpassung Backend-Code

Zu diesem Feature gehören im Kern die Klassen `CesSendPasswordResetInstructionsAction` und `PmConfiguration`.

Die Klasse `CesSendPasswordInstructions` erweitert die Original-Klasse vom CAS und überschreibt die entsprechende
Methode - in diesem Fall die Methode `doExecute`. Praktisch kann hier erstmal der Original-CAS Code kopiert und
dann entsprechend angepasst werden. Um möglichst duplizierten Code zu vermeiden, sollte - sofern möglich - nicht die
gesamte Methode kopiert werden, sondern weiterhin die super-Methode verwendet und aufgerufen werden.

In der Klasse `PmConfiguration` wird die Bean erzeugt. Auch hier ist der Code größtenteils kopiert. Lediglich wird hier
statt einer Instanz von `SendPasswortInstructions` eine Instanz von unserer eigenen Klasse `CesSendPasswordInstructions`
erzeugt.

Damit die Erzeugung der Bean in der Klasse `PmConfiguration` auch angezogen wird, muss diese Klasse in der Datei
`spring.factories` und `resources/META-INF` eingetragen werden.

Bei Klassen, die eine CAS-Klasse erweitern, bietet sich als Namens-Konvention an `Ces` an den Anfang des Klassennamens
zu setzen. So können die von uns erweiterten und angepassten Klassen schnell identifiziert werden.

#### Möglichkeit für Unit-Tests

Um den angepassten Code zu testen, können die Beans gemockt werden.

Etwas schwieriger wird es, wenn die zu testende Methode, eine komplexere super-Methode aufruft. Für diesen Fall gibt es
einen Workaround. Die Klasse, in der sich die zu testende Methode befindet, wird für den überschriebenen Test erweitert
und die aufgerufene super-Methode wird überschrieben. Dabei liefert die überschriebene-Methode einfach nur einen
bestimmten Wert zurück.

Ein Beispiel hierfür in der Klasse `CesSendPasswordResetInstructionsActionTest` zu finden.

#### Anpassung Frontend-Code

Die Texte an der Oberfläche des CAS sind für die jeweilige Sprache als Propertys in einer `.properties`-Datei abgelegt.

Um den Hinweistext, dass eine E-Mail verschickt worden ist, anzupassen und zu erweitern, müssen die entsprechenden
Text-Propertys auf Deutsch und auf Englisch angepasst werden.

Der entsprechende Property-Key für den Hinweis-Test ist `screen.pm.reset.sentInstructions`. Um den Text hierfür zu
überschreiben, muss in den Properties-Dateien `custom_messages.properties` und `custom_messages_de.properties` im `app`
-Verzeichnis unter `src/main/resources` der Key hinzugefügt und der entsprechende Text als Wert abgelegt werden.

Bei den deutschen Texten ist darauf zu achten, dass Umlaute codiert angegeben werden, z.B. `\u00FC` für ein `ü`

Nach Anpassung des Textes - der Text ist nun länger geworden - ist zu überprüfen, ob dieser noch vernünftig in der
Oberfläche dargestellt wird. Dies ist in diesem Beispiel der Fall. Wäre dies nicht der Fall, hätten noch Änderungen an
dem entsprechenden Thymeleaf-Template oder den HTML-Fragment erfolgen müssen.

#### Data-TestIDs für Integrationstests

Wenn Integrationstests angelegt werden, ist es sinnvoll in den HTML-Elementen eine eindeutige Data-TestID zu
hinterlegen. Mit Hilfe dieser ID kann der Integrationstests dann eindeutig auf ein Element zugreifen und es müssen
keine Anpassungen an den Integrationstests vorgenommen werden, wenn sich etwas anderes an dem HTML-Element ändert.