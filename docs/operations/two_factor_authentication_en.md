# Two-Factor Authentication

> ️⚠ **Warning**
>
> This feature is **experimental** and may cause problems when accessing other Dogus or Dogu APIs, especially when Basic Auth is used.

CAS supports two-factor authentication with TOTP (Time-based One-Time Password). After activation,
all employees need an authenticator app such as Google Authenticator or Microsoft Authenticator to log in.

## Activation

The corresponding Dogu configuration key must be set to `true`. The Dogu must be restarted for the configuration to take effect.

``kubectl edit -n ecosystem configmap cas-config``
`````yaml
  data:
    config.yaml: |
      experimental:
        totp:
          activate:
  `````

## Deactivation

If there are problems with two-factor authentication, the feature can be disabled. To do this, the corresponding
Dogu configuration key must be set to `false`. Disabling it does not regenerate the internal encryption codes.
The Dogu must be restarted for the configuration to take effect.

``kubectl edit -n ecosystem configmap cas-config``
```yaml
  data:
    config.yaml: |
      experimental:
        totp:
          activate: false
```
