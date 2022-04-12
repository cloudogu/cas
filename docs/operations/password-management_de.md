# Passwort Management im CAS

Im CAS ist das Passwort-Management aktiviert, sodass Nutzer bei einem abgelaufenen Passwort direkt innerhalb des CAS ein
neues Passwort vergeben können. Dieses Feature ist nur aktiv, wenn ein `embedded`-LDAP (also das LDAP-Dogu) verwendet
wird.

## Funktionsweise des Passwort Managements im CAS

Wenn sich ein Nutzer mit einem abgelaufenen Passwort einloggt, so wird dieser auf eine Seite weitergeleitet, auf der
dieser sein Passwort ändern kann. Die Änderung des Passworts erfolgt direkt im konfigurierten LDAP. Dies ist möglich, da
der verwendete Service-Account für das LDAP Schreibrechte im LDAP hat.

Nach Änderung des Passworts wird der Nutzer auf eine Bestätigungsseite weitergeleitet und muss sich anschließend einmal
neu mit seinem nun geänderten Passwort anmelden.

## Konfiguration des Passwort Managements im CAS

Das Passwort-Management wird über bestimmte CAS-Properties aktiviert. Für die allgemeine Aktivierung des
Passwort-Managements sind beiden folgenden Propertys erforderlich.

* `cas.authn.pm.enabled=true` - gibt mit dem Wert `true` an, dass das Passwort-Management aktiviert ist.
* `cas.authn.pm.policy-pattern=.*` - gibt mit dem Wert `.*` (Regulärer Ausdruck) an, dass alle Passwörter gültig sind.
  Diese Property ist verpflichtend und muss auch dann angegeben werden, wenn es keine Einschränkungen für das Passwort
  gibt.

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