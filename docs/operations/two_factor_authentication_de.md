# Zwei-Faktor-Authentifizierung

> ️⚠ **Achtung**
> 
> Diese Funktion ist **experimentell** und kann zu Problemen beim Zugriff auf andere Dogus oder die API von Dogus führen, vor allem wenn Basic Auth verwendet wird.

CAS unterstützt Zwei-Faktor-Authentifizierung mit TOTP (Time-based One-Time Password). Nach der Aktivierung benötigen 
alle Mitarbeitenden eine Authenticator-App wie z.B. Google Authenticator oder Microsoft Authenticator, um sich anzumelden.

## Aktivierung

Der zugehörige Dogu-Konfigurationsschlüssel muss auf `true` gesetzt werden.

```shell
etcdctl set config/cas/experimental/totp/activate "true"
```

Das Dogu muss neu gestartet werden, damit die Konfiguration wirksam wird.

```shell
cesapp restart cas
```

## Deaktivierung

Bei Problemen mit der Zwei-Faktor-Authentifizierung kann die Funktion deaktiviert werden. Dafür muss der zugehörige 
Dogu-Konfigurationsschlüssel auf `false` gesetzt werden. Durch eine Deaktivierung werden die internen Verschlüsselungscodes
nicht neu generiert.

```shell
etcdctl set config/cas/experimental/totp/activate "false"
```

Das Dogu muss neu gestartet werden, damit die Konfiguration wirksam wird.

```shell
cesapp restart cas
```