Two-factor authentication

> ️⚠ **Warning**
>
> This feature is **experimental** and may cause problems when accessing other Dogus or the Dogus API, especially when using Basic Auth.

CAS supports two-factor authentication with TOTP (Time-based One-Time Password). Once activated,
all employees will need an authenticator app such as Google Authenticator or Microsoft Authenticator to log in.

## Activation

Set the global configuration key `2fa/activate` to `true`.

```shell
etcdctl set /config/_global/2fa/activate "true"
```

Restart the Dogu to activate the configuration.

```shell
cesapp restart cas
```

## Deactivation

If you encounter problems with two-factor authentication, you can deactivate the function. To do so, set the corresponding
global configuration key to `false`. Deactivation means that the internal encryption codes
will not be regenerated.

```shell
etcdctl set /config/_global/2fa/activate "false"
```

Restart the Dogu to activate the configuration.

```shell
cesapp restart cas
```