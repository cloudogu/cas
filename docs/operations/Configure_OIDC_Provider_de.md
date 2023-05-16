# Konfiguration zur Verwendung eines OIDC Providers als Authentifizierungsquelle

Im folgenden Abschnitt werden die Konfigurationseinträge für die Einrichtung eines OIDC Providers zur Authentifizierung im CAS beschrieben.

## Voraussetzungen

Es ist ein funktionaler OIDC Provider vorhanden. In diesem Provider muss ein neuer Client registriert werden. 
Eine solche Registrierung besteht grundsätzlich aus einer Client-ID und einem Client-Secret.

## Konfiguration

#### oidc/enabled
* Konfiguration-Schlüssel-Pfad: `<cas_path>/oidc/enabled`
* Inhalt: Legt fest, ob das CAS den konfigurierten OIDC-Anbieter für die delegierte Authentifizierung verwenden soll.
  Dieser Eintrag ist standardmäßig auf 'false' gesetzt.
* Datentyp: Wahrheitswert
* Valide Werte: `true`, `false`

#### oidc/discovery_uri
* Konfiguration-Schlüssel-Pfad: `<cas_path>/oidc/discovery_uri`
* Inhalt: Beschreibt den URI, der die Beschreibung für das OIDC-Protokoll des Zielanbieters enthält. 
  Muss auf die openid-connect Konfiguration zeigen. 
  Diese ist meist wie folgt aufgebaut: `https://[base-server-url]/.well-known/openid-configuration`
* Datentyp: String
* Valide Werte: `https://[base-server-url]/.well-known/openid-configuration`

#### oidc/client_id
* Konfiguration-Schlüssel-Pfad: `<cas_path>/oidc/client_id`
* Inhalt: Enthält die Kennung, die zur Identifizierung des CAS gegenüber dem OIDC-Anbieter verwendet werden soll.
* Datentyp: String

#### oidc/client_secret
* Konfiguration-Schlüssel-Pfad: `<cas_path>/oidc/client_secret`
* Inhalt: Enthält das Geheimnis, das zusammen mit der Client-ID verwendet werden soll, um den CAS gegenüber dem OIDC-Anbieter zu identifizieren.
* Datentyp: String
* **Verschlüsselt**

#### oidc/display_name
* Konfiguration-Schlüssel-Pfad: `<cas_path>/oidc/display_name`
* Inhalt: Der Anzeigename wird für den OIDC-Anbieter auf der Benutzeroberfläche verwendet.
* Datentyp: String

#### oidc/redirect_uri
* Konfiguration-Schlüssel-Pfad: `<cas_path>/oidc/redirect_uri`
* Inhalt: Legt fest, an welche Adresse nach dem Logout umgeleitet werden soll.
* Datentyp: String
* Standardwert: `<FQDN>/cas/logout`

#### oidc/optional
* Konfiguration-Schlüssel-Pfad: `<cas_path>/oidc/optional`
* Inhalt: Legt fest, ob die Authentifizierung über den konfigurierten OIDC-Anbieter optional ist. 
  Der Benutzer wird automatisch auf die Anmeldeseite des OIDC-Anbieters umgeleitet, wenn diese Eigenschaft auf 
  'false' gesetzt ist. Der Eintrag 'true' macht die Authentifizierung über den OIDC-Provider optional. 
  Dazu wird auf der Login-Seite des CAS eine zusätzliche Schaltfläche für den OIDC-Provider angezeigt, 
  über die eine Authentifizierung beim OIDC-Provider erfolgen kann. Standardmäßig ist dieser Eintrag auf 'false' gesetzt.
* Datentyp: Wahrheitswert
* Valide Werte: `true`, `false`

#### oidc/scopes
* Konfiguration-Schlüssel-Pfad: `<cas_path>/oidc/scopes`
* Inhalt: Gibt die Ressource an, die gegen OIDC abgefragt werden soll. 
  Normalerweise sollte diese Aufzählung mindestens die openid, die E-Mail des Benutzers, 
  Profilinformationen und die dem Benutzer zugewiesenen Gruppen enthalten. Dieser 
  Eintrag ist standardmäßig auf "openid email profile groups" gesetzt.
* Datentyp: String nach Format: `scope1 scope2`

#### oidc/attribute_mapping
* Konfiguration-Schlüssel-Pfad: `<cas_path>/oidc/attribute_mapping`
* Inhalt: Die von OIDC bereitgestellten Attribute stimmen nicht 
  genau mit den vom CAS geforderten Attributen überein. 
  Es ist notwendig, die OIDC-Attribute in vom CAS akzeptierte 
  Attribute umzuwandeln. Daher sollte dieser Eintrag Regeln zur 
  Umwandlung eines vom OIDC-Anbieter bereitgestellten Attributs in ein 
  vom CAS erforderliches Attribut enthalten. Die Regeln sollten in 
  folgendem Format angegeben werden: email:mail,familienname:nachname'. 
  In dem angegebenen Beispiel werden die OIDC-Attribute "email" und "family_name" in "mail" 
  bzw. "surname" umgewandelt. Das CAS benötigt die folgenden Attribute, 
  um ordnungsgemäß zu funktionieren: "mail,surname,givenName,username,displayName".
* Datentyp: String nach Format: `fromAttribute:toAttribute,fromAttribute2:toAttribute2`

#### oidc/principal_attribute
* Konfiguration-Schlüssel-Pfad: `<cas_path>/oidc/principal_attribute`
* Inhalt: Gibt ein Attribut an, das als Haupt-ID innerhalb des CES verwendet werden soll. 
  CAS verwendet die vom OIDC-Anbieter bereitgestellte ID, wenn diese Eigenschaft leer ist.
* Datentyp: Name eines OIDC attribute