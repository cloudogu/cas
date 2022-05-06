# Passwort Management im CAS

Im CAS ist das Passwort-Management aktiviert. So können Nutzer bei einem abgelaufenen Passwort direkt innerhalb des CAS
ein neues Passwort vergeben. Ebenso können Nutzer sich per E-Mail einen Link zum Neusetzen ihres Passworts zuschicken
lassen, wenn sie ihr Passwort vergessen haben.

Diese Features sind nur aktiv, wenn ein `embedded`-LDAP (also das LDAP-Dogu) verwendet wird.

## Features des Passwortmanagements

### Passwortänderung bei abgelaufenen Passwort

Wenn sich ein Nutzer mit einem abgelaufenen Passwort einloggt, so wird dieser auf eine Seite weitergeleitet, auf der
dieser sein Passwort ändern kann. Die Änderung des Passworts erfolgt direkt im konfigurierten LDAP. Dies ist möglich, da
der verwendete Service-Account für das LDAP Schreibrechte im LDAP hat.

Nach Änderung des Passworts wird der Nutzer auf eine Bestätigungsseite weitergeleitet und muss sich anschließend einmal
neu mit seinem nun geänderten Passwort anmelden.

### Passwort über E-Mail verschicktem Link zurücksetzen

Über die `Passwort zurücksetzen`-Funktion kann ein Nutzer sich per E-Mail einen Link zum Zurücksetzen seines Passworts
zuschicken lassen. Der Link wird nach Eingabe seines Nutzernamens an die im LDAP hinterlegte E-Mail-Adresse des Nutzers
geschickt. Über diesen Link gelangt der Nutzer auf eine Seite im CAS, über die er ein neues Passwort vergeben kann.

## Konfiguration des Passwort Managements im CAS

Das Passwort-Management wird über bestimmte CAS-Properties aktiviert. Für die allgemeine Aktivierung des
Passwort-Managements sind beiden folgenden Propertys erforderlich.

* `cas.authn.pm.core.enabled=true` - gibt mit dem Wert `true` an, dass das Passwort-Management aktiviert ist.
* `cas.authn.pm.core.password-policy-pattern=.*` - gibt mit dem Wert `.*` (Regulärer Ausdruck) an, dass alle Passwörter
  gültig sind. Diese Property ist verpflichtend und muss auch dann angegeben werden, wenn es keine Einschränkungen für
  das Passwort gibt.

Damit der Nutzer direkt über das CAS sein Passwort im LDAP ändern kann, müssen hierzu die entsprechenden LDAP-Property
für das Passwort-Management gesetzt werden. Diese Propertys können die Werte der allgemeinen LDAP-Property
referenzieren.

* cas.authn.pm.ldap[0].type - die Variante des LDAPs. Für das LDAP-Dogu ist dies `GENERIC`.
* cas.authn.pm.ldap[0].ldap-url - die URL des LDAPs.
* cas.authn.pm.ldap[0].base-dn - der zu verwendende base DN (Distinguish Name) des LDAPs. Der base DN gibt den LDAP
  Eintrag an, unter welchem Eintrag die zu authentifizierenden Nutzer zu finden sind.
  Beispiel: `ou=People,o=ces.local,dc=cloudogu,dc=com`. Hier werden die Einträge, die der Organistionseinheit (
  OU) `people` zugeordnet sind, berücksichtigt.
* cas.authn.pm.ldap[0].search-filter - der Filter für die Suche nach Nutzern.
  Beispiel: `(&(objectClass=person)(uid={user}))`. Dieser Suchfilter sucht nach Einträgen mit der Objektlasse Person
  anhand der User ID.
* cas.authn.pm.ldap[0].bind-dn - der Bind-DN des Nutzers, der bei der Verbindung mit LDAP verwendet werden soll. Dieser
  Nutzer nimmt die Änderungen im LDAP vor.
* cas.authn.pm.ldap[0].bind-credential - die Anmeldeinformationen (das Passwort), die bei der Verbindung zum LDAP und
  verwendet werden sollen.

Für den Versand des Links zum Zurücksetzen des Passworts müssen neben der allgemeinen Einrichtung des E-Mail-Versands
die folgenden Propertys gesetzt werden:

* cas.authn.pm.reset.mail.attribute-name - gibt den Namen des Mail-Attributes im LDAP an. Dieser Wert wird aus dem
  etcd-Eintrag `ldap/attribute_mail` ausgelesen.
* cas.authn.pm.reset.mail.from - gibt die E-Mail-Adresse an, die als Absender der E-Mail angezeigt wird. Dieser Wert
  kann über den etcd-Eintrag `mail_sender` konfiguriert werden. Wird kein Wert angegeben, wird ein Default-Wert
  verwendet.
* cas.authn.pm.reset.mail.subject - gibt den Betreff der E-Mails an. Dieser Wert kann über den
  etcd-Eintrag `password_management/reset_password_subject` konfiguriert werden. Wird kein Wert angegeben, wird ein
  Default-Wert verwendet.
* cas.authn.pm.reset.mail.text - gibt den Text der E-Mail an. Dieser Wert kann über den
  etcd-Eintrag `password_management/reset_password_text` konfiguriert werden. Es ist zwingend erforderlich, dass in dem
  Text ein `%s` als Platzhalter für den Passwort-Zurücksetzen-Link enthalten ist. Ist im etcd kein Wert angegeben, wird
  ein Default-Wert verwendet.
* cas.authn.pm.reset.expiration - definiert die Dauer für die Gültigkeit des Passwort-Zurücksetzen-Links. Die Angabe
  erfolgt in der `java.time.Duration`-Syntax
* cas.authn.pm.reset.security-questions-enabled - gibt mit `false` an, dass zum Zurücksetzen des Passworts keine
  Sicherheitsfragen beantwortet werden müssen

### Deaktivierung der Passwort-Zurücksetzen-Funktion

Es besteht die Möglichkeit, die Passwort-Zurücksetzen-Funktion durch Setzen eines entsprechenden etcd-Eintrags zu
deaktivieren. Um die Passwort-Zurücksetzen-Funktion zu deaktivieren, muss der
Wert `password_management/enable_password_reset_via_email` auf`false` gesetzt werden.

Anstelle des Links `Passwort zurücksetzen` wird stattdessen der Button `Passwort vergessen` angezeigt - sofern im etcd
ein Wert für den Eintrag `forgot_password_text` hinterlegt ist. Wenn ein Nutzer auf den `Passwort vergessen`-Button
klickt, wird der unter `forgot_password_text` hinterlegte Text angezeigt. 