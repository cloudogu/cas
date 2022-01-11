# CAS Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Fixed
- The name entered at login previously has been directly transferred to the session (including upper and lower case).
  This has led to some problems and has now been changed to use the name and spelling from the LDAP entry. (#133)
- On the logout page, English text was displayed in the German version. The correct German text is now displayed here 
  for the English text.

## [v6.3.7-1] - 2021-12-20
### Changed
- Update cas overlay version to version 6.3.7.4 (#129)

## [v6.3.3-11] - 2021-12-13
### Fixed
- jndi vulnerability by updating log4j to 2.15.0 (#126, https://www.heise.de/news/Kritische-Zero-Day-Luecke-in-log4j-gefaehrdet-zahlreiche-Server-und-Apps-6291653.html) #126

## [v6.3.3-10] - 2021-11-30
### Fixed
- Get CAS 4 upgrade compatibility by moving upgrade steps to post-upgrade script; #123

## [v6.3.3-9] - 2021-11-30

### Added
- add testkeys in thymeleaf templates for a stable selection in integrationtests (#122)

## [v6.3.3-8] - 2021-11-09
### Changed
- warning label for invalid credentials conforms to styleguide (#120)

## [v6.3.3-7] - 2021-10-21
### Changed
- use equal login error messages (#118)

## [v6.3.3-6] - 2021-10-20
### Changed
- correct font-stack for inputs (#116)
- update ces-theme to v0.4.0

## [v6.3.3-5] - 2021-10-06
### Added
- OIDC-client support. Now, it is possible to register OIDC clients at the CAS via a service account. For more information see [docs](docs/operations/oauth_guide_en.md) (#114)

## [v6.3.3-4] - 2021-09-24
### Added
- OIDC-property to define an attribute that should be used as the principal id for the clients (#112)

## [v6.3.3-3] - 2021-09-20
### Fixed
- CAS could not handle fqdn that contain uppercase letters (#110)

## [v6.3.3-2] - 2021-09-09
### Added
- Add new configuration keys to delegate the cas authentication to a configured OIDC provider. For more information about the keys see [here](./docs/operations/Configure_OIDC_Provider_en.md) (#107)

### Changed
- Update UI to show OIDC-Link (#108)

## [v6.3.3-1] - 2021-08-31
### Added
- Add new LDAP specific dogu configuration keys (#99)
- Re-add LDAP group resolving with internal resolvers (#99)

### Changed
- Adapt the UI to the Cloudogu styling. (#91)
- Update the underlying Tomcat library to v9.0 (#36)

### Removed
- Remove dependency to the ldap-mapper dogu in favour of direct LDAP connections (#99)
  - The vision of abstracting LDAP connections with help of the [ldap-mapper](https://github.com/cloudogu/ldap-mapper) dogu still remains. This change is an intermediate step until the necessary changes to the ldap-mapper dogu and the migration towards CAS 6 are completed.
- Remove dogu configuration key ```ldap/use_user_connection_to_fetch_attributes```. From now on, all connections to the LDAP to fetch user attributes are made via the system connection. (#103)

### Fixed
- At log level debug, the password has been output in plain text in some classes. The password is now no longer output 
  in plain text anywhere. (#86)

### Removed
- Remove dogu configuration key `logging/translation_messages`

## [v4.0.7.20-20] - 2021-06-02
### Changed
- Add autofocus attribute to username (#83)

## [v4.0.7.20-19] - 2021-05-06

### Changed
- Improve accessibility of login mask by changing design (#80)

## [v4.0.7.20-18] - 2021-04-20

### Changed
- Changes the positioning of alert-fields and the login button

## [v4.0.7.20-17] - 2021-04-19

### Changed
- Changes to button and alert-dialogues on the login- and logout-page to increase accessibility

## [v4.0.7.20-16] - 2021-02-18
### Added
- Adds verification via OAuth to the CAS
### Fixed
- Return empty service account list if account directory is missing in registry

## [v4.0.7.20-15] - 2021-01-22
### Changed
- Add own log level configuration for translation logs; #64
- Set default log level for translation related logs to ERROR; #64

## [v4.0.7.20-14] - 2021-01-11
### Fixed
- Activate Perf4J logger only if log level is INFO or DEBUG; #62

## [v4.0.7.20-13] - 2020-12-17
### Fixed
- bug where the `forgot_password_text`-key was never applied for some browsers (#60)

### Changed
- Update java base image to 8u252-1

## [v4.0.7.20-12] - 2020-12-14
### Added
- Ability to set memory limit via `cesapp edit-config`
- Ability to configure the `MaxRamPercentage` and `MinRamPercentage` for the CAS process inside the container via `cesapp edit-conf` (#58)

## [v4.0.7.20-11] - 2020-11-19
### Added
- add locales for de_DE and en_US

### Fixed
- change server encoding so special characters will be decoded correctly (#56)

## [v4.0.7.20-10] - 2020-11-12
### Fixed
- CAS bloated the log file after a dogu was uninstalled or marked as `absent` during a blueprint upgrade. (#54)

## [v4.0.7.20-9](https://github.com/cloudogu/cas/releases/tag/v4.0.7.20-9) - 2020-07-24
### Changed
- Use doguctl validation for log level

### Added
- Add modular makefiles
- Add automated release flow

## [v4.0.7.20-8](https://github.com/cloudogu/cas/releases/tag/v4.0.7.20-8) - 2020-04-08
### Added 

A new CES registry key `logging/root` is evaluated to override the default root log level (#49). One of these values can be set in order to increase the log verbosity: `ERROR`, `WARN`, `INFO`, `DEBUG`. 

CAS's Log4J log levels are directly applied from the root log level.

Tomcat log levels are mapped from the root log level as follows:

| root log level | Tomcat log level |
|----------------|------------------|
| ERROR | Everything equal or above ERROR |
| WARN  | Everything equal or above WARNING |
| INFO  | Everything equal or above INFO |
| DEBUG | Everything equal or above FINE |

### Changed
- Under the hood we verify the Tomcat binary to spot (possibly) tampered Tomcat binaries during the build time. (#38)

### Fixed
- PerformanceStats are no longer logged to the container filesystem for reasons of discoverability and performance. Instead they are logged to the usual CES logging facility. (#48)

## [v4.0.7.20-7](https://github.com/cloudogu/cas/releases/tag/v4.0.7.20-7) - 2020-03-12 
### Added
* cas config etcd key `session_tgt/max_time_to_live_in_seconds` to configure maximum session timeout
* cas config etcd key `session_tgt/time_to_kill_in_seconds` to configure idle session timeout
