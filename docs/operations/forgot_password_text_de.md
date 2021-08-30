# Konfiguration des Textes für vergessenes Passwort

Es kann eine Benutzerdefinierte Nachricht angezeigt werden, die erscheint, 
wenn auf "Passwort vergessen?" geklickt wird. Dort können Informationen hinterlegt 
werden, wie ein Nutzer damit umgehen soll, wenn dieser sein Passwort vergessen hat.

Dafür muss nur der etcd key `config/cas/forgot_password_text` auf den gewünschten Wert
gesetzt werden und das CAS Dogu neu gestartet werden. Der Text kann nun in der Login Maske
durch einen Klick auf "Passwort vergessen?" ein- und ausgeblendet werden.
