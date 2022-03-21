{{if .Config.Exists "legal_urls/terms_of_service" }}
legal.url.terms.of.service={{ .Config.Get "legal_urls/terms_of_service"}}
{{end}}
{{if .Config.Exists "legal_urls/imprint" }}
legal.url.imprint={{ .Config.Get "legal_urls/imprint"}}
{{end}}
{{if .Config.Exists "legal_urls/privacy_policy" }}
legal.url.privacy.policy={{ .Config.Get "legal_urls/privacy_policy"}}
{{end}}
{{if .Config.Exists "forgot_password_text" }}
forgot.password.text={{ .Config.Get "forgot_password_text"}}
{{end}}
