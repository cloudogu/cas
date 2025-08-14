# CAS Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [v7.1.6-5] - 2025-08-13
### Fixed
- [#273] Remove expired sessions that stay active by configuring an interval-based registry cleaner

## [v7.0.10-4] - 2025-08-13
### Fixed
- [#273] Remove expired sessions that stay active by configuring an interval-based registry cleaner

## [v7.1.6-4] - 2025-07-28
### Fixed
- [#271] Fix the wrong LogoutURL after changing the global FQDN

## [v7.1.6-3] - 2025-05-09
### Fixed
- [#269] Fix CAS 3 success view for Redmine missing formattedAttributes due to incomplete model
- [#266] Fix destroying the oidc-session on logout
    - The "oauthLogoutExecutionPlanConfigurer"-bean was overwritten by the "CesOAuthConfiguration" which did not destroy the OIDC-session on logout.
    - Now there is "cesOAuthLogoutExecutionPlanConfigurer" which does not overwrite the default behaviour.

## [v7.1.6-2] - 2025-05-08
### Changed
- Fix: Injects the missing `pgtUrl` attribute into the model in `cas3ServiceSuccessView` to properly render the `<cas:proxy>` element in the CAS 3.0 service ticket response.

## [v7.1.6-1] - 2025-04-30
### Changed
- [#263] Upgrade CAS to 7.1.6
- [#263] Spring Security `spring-security-web` from `6.2.7` → `6.3.4`
- [#263] Spring Boot Version: `3.2.5` → `3.3.9`
- [#263] Anpassung Delegated Authentication: OIDC Clients müssen jetzt explizit über eigene Factory und Provider registriert werden ([DelegatedClientOidcBuilder](https://github.com/apereo/cas/blob/7.1.x/support/cas-server-support-pac4j-oidc/src/main/java/org/apereo/cas/pac4j/web/DelegatedClientOidcBuilder.java))
- [#263] Service Registry behavior adapted to support incomplete JSON service definitions (templateName-only)
  - Implemented `CesAbstractResourceBasedServiceRegistry` to override strict name/serviceId filtering introduced in CAS 7.1.x ([AbstractResourceBasedServiceRegistry.java#L316](https://github.com/apereo/cas/blob/ad11196459b072e3242be77dcd95251bc2f64499/core/cas-server-core-services-registry/src/main/java/org/apereo/cas/services/resource/AbstractResourceBasedServiceRegistry.java#L316))
  - Introduced `CesLegacyCompatibleTemplatesManager` to correctly apply templates for partial service definitions
  - Configured Spring Boot beans for custom Service Registry and Template Manager

## [v7.0.10-2] - 2025-04-23
### Changed
- [#257] Set sensible resource requests and limits

## [v7.0.10-1] - 2025-04-17
### Changed
- [#261] Upgrade CAS to 7.0.10.1
### Security
- [Fix OIDC WebAuthn Vulnerability](https://apereo.github.io/2025/04/11/oidc-webauthn-vuln/) –   OIDC-Backchannel Authentication with WebAuthn could allow privilege escalation under specific circumstances  

## [v7.0.8-14] - 2025-04-16
### Changed
- Spring Security `spring-security-web` from `6.2.1` → `6.2.7`
- Spring Boot Version: `3.2.1` → `3.2.5`
- Tomcat `tomcat-catalina` from `10.1.34` → `10.1.39`
- Removed `tomcat-embed-core` Version `10.1.29` reference 
- Makefiles updated to Version `v9.9.1`
- `ces-build-lib` updated to Version `4.2.0`
- `dogu-build-lib` updated to Version `3.2.0`

### Security
- [Fix CVE-2024-38821](https://nvd.nist.gov/vuln/detail/CVE-2024-38821) – Spring Security Authorization Bypass in WebFlux
- [Fix CVE-2025-24813](https://nvd.nist.gov/vuln/detail/CVE-2025-24813) – Apache Tomcat: HTTP/2 stream handling vulnerability
- [Fix OIDC WebAuthn Vulnerability](https://apereo.github.io/2025/04/11/oidc-webauthn-vuln/) –   OIDC-Backchannel Authentication with WebAuthn could allow privilege escalation under specific circumstances  


## [v7.0.8-13] - 2025-04-07
### Fixed
- Use default values for keys `ldap/attribute_given_name` and `ldap/attribute_surname` if they are configured as empty strings.

## [v7.0.8-12] - 2025-03-12
### Changed 
- [#248] Cleanup old service-accounts from JSON-registry before creating a new service-account

## [v7.0.8-11] - 2025-02-12
### Fixed
- Use correct configuration keys `ldap/attribute_given_name` and `ldap/attribute_surname` in cas properties.

## [v7.0.8-10] - 2025-01-10
### Added
- Add configuration for `allowed_groups` and `initial_admin_user` in delegated authentication

### Changed
- [#246] Update base image to Alpine 3.21.0 and Java to 21.0.5-p11
- Update Tomcat to 10.1.34

### Fixed
- [#246] Fix a restart loop if the config key `oidc/enabled` was not set.

## [v7.0.8-9] - 2024-12-20
### Added
- Add http health-check, so that the dogu will get healthy when the start of the web-application is completed

### Fixed
- Fix start under high system load [#240]

## [v7.0.8-8] - 2024-12-20
### Changed
- Password reveal button to checkbox for better readability [#236]
### Added
- Integrationtest for password reveal

## [v7.0.8-7] - 2024-12-16
### Fixed
- Fix post upgrade script when no service account is defined [#242]
- Remove hashes from validated service url [#238]
  - Makes cockpit dogu links available again

## [v7.0.8-6] - 2024-11-25
### Fixed
- Prevent logging of invalid login credentials [#233]

### Added
- Integration test searching for unencrypted passwords in the cas logs [#225]

## [v7.0.8-5] - 2024-11-21
### Breaking Change
- Newly installed dogus must explicitly request the creation of a service account in the CAS via dogu.json. Further information on this can be found in the [developer documentation](https://github.com/cloudogu/dogu-development-docs/blob/main/docs/important/relevant_functionalities_en.md#authentifizierung)

### Changed
- Use JSON service registry [#221]
  - services are read from and stored in json files instead of local config
  - native implementation from CAS is used for this, which reduces custom overlay implementation
- Changed logic to create and remove service accounts [#221]

### Removed
- Reading service information directly from ETCD [#221]
  - Removed java classes for service creation

### Fixed
- Fix ServiceIdFQDN regex by changing illegal url characters [#228]


## [v7.0.8-4] - 2024-11-13
### Added
- Replicate users from delegated authentication into LDAP [#224]
  - delegated authentication currently only works when using the embedded LDAP 
- Disclaimer for legal_urls without protocol [#230]

### Fixed
- Fix configuration for delegated authentication with OIDC [#222]

## [v7.0.8-3] - 2024-10-11
### Changed
- Use flat instead of nested attributes for OAuth user profile. [#219]
### Fixed
- OIDC- and OAuth-Dogus which relied on a flat OAuth user profile structure were unable to parse the user profile. [#219]

## [v7.0.8-2] - 2024-10-02
### Fixed
- Fix a bug where the watch for service accounts in the config `local.yaml` stucks because the events wasn't resetted and polled [#217]

## [v7.0.8-1] - 2024-09-23
### Added
- Add "lang"-attribute to HTML-Pages [#213]
- update CAS to 7.0.8

## [v7.0.5.1-8] - 2024-09-18
### Changed
- Relicense to AGPL-3.0-only

## [v7.0.5.1-7] - 2024-09-05
### Fixed
- The pre-upgrade-script will no longer try to access the node_master-file for all migrations [#211]
  - The access to the node_master-file has been moved to the migration where it es needed and where the node_master-file was still present. 

## [v7.0.5.1-6] - 2024-08-27
### Fixed
- The post-upgrade-script would fail in a specific edge-case situation [#207]
  - Affected System: Cloudogu EcoSystem 'Classic' (Pre-Multinode)
  - Affected versions: `7.0.5.1-4` and `7.0.5.1-5`
  - When an OAuth/OIDC-Dogu was installed and then uninstalled, the post-upgrade script would fail during an upgrade from CAS versions below `7.0.5.1-4`.
  - This means that a directory `/config/cas/service_accounts/<type>` had to exist, but be empty (where `<type>` can be `oauth` or `oidc`).
  - Additionally prevent similar cases.

## [v7.0.5.1-5] - 2024-08-20
### Fixed
- Add missing data-testid to logout error messages [#203]
- Add missing translations

## [v7.0.5.1-4] - 2024-08-20
### Changed
- In a single-node EcoSystem (Classic-CES):
  - Use explicit service accounts for normal CAS service accounts _in addition to_ implicitly reading dependencies. (#197)
  - Receive logout URI as an additional argument in the `service-account-create` exposed command _in addition to_ reading it from the dogu descriptor. (#197)
- In a multi-node EcoSystem:
  - Use explicit service accounts for normal CAS service accounts _instead of_ implicitly reading dependencies. (#197)
  - Receive logout URI as an additional argument in the `service-account-create` exposed command _instead of_ reading it from the dogu descriptor. (#197)
- Use config from mounted file when in multinode (#197)
- Upgrade java base image to `21.0.4-1` (#193)
- Replace persistent state with local config (#193)

## [v7.0.5.1-3] - 2024-08-16
### Fixed
- Fix design of login mask to be more flexible

## [v7.0.5.1-2] - 2024-08-15
### Changed
- Modify the whole cas ui to match with our new theme (#201) 

## [v7.0.5.1-1] - 2024-07-23
### Fixed
- Fix throttling and avoid rendering CES unusable (#198) 
  - when intensely sending REST requests towards Nexus with an
    internal user

### Changed
- migrate dogu configuration keys regarding throttling (#198)
  - with CAS 7.x, throttling works way differently than before, and with different configuration keys as well
  - `limit/max_numbers` no longer enables throttling in general
    - now, `limit/failure_threshold` with a value other than zero takes it place. With a default value of `500` login failures / timeframe, it will receive a much higher default value though.
  - `limit/range_seconds` is a new configuration key. It defines the timeframe in seconds and is used to build a failure rate with `limit/failure_threshold`.
  - `limit/stale_removal_interval` is a new key which configures now the interval in seconds of when a background runner runs to remove stale login failures. The interval is independent of `limit/failure_threshold` 
- update CAS to 7.0.5.1
- update Tomcat to 10.1.26

### Removed
- remove the custom throttling implementation in favor of the original CAS 7 failure throttling implementation (#198)

### Security
- Fix OAuth/OpenID vulnerability: see https://apereo.github.io/2024/06/26/oidc-vuln/

## [v7.0.4.1-2] - 2024-07-09
### Fixed
- Fix a bug where CAS logs the password in debug log level (#195)

## [v7.0.4.1-1] - 2024-06-06
### Changed
- Upgrade CAS to 7.0.4.1 (#189)
  - CAS 7.0 contains a "weak password detection" that checks on every login if the password complies with the configured password-rules.
    If a password does not comply a warning is displayed and the user has to enter a new password that complies with the rules.

## [v6.6.15.1-1] - 2024-05-24
### Changed
- Upgrade CAS to 6.6.15.1 (#190)

### Security
- see https://apereo.github.io/2024/05/18/oauth-vuln/


## [v6.6.15-1] - 2024-02-15
### Changed
- Upgrade CAS to 6.6.15 

### Security
- spring-security-core: CVE-2022-31692 / CVE-2023-20862


## [v6.6.12-2] - 2024-02-06
### Added
- add new volume `/logs` to avoid logging into the container file system (#173)

### Changed
- generated log files now reside under `/logs` instead of `/opt/apache-tomcat/logs` (#173)

### Fixed
- log files no longer spam the container file system which lead to resource exhaustion in the host file system (#173)

## [v6.6.12-1] - 2023-09-21
### Changed
- Update CAS to 6.6.12 to fix a OpenID-Connect and OAuth vulnerability (#184)
  - see https://apereo.github.io/2023/09/14/oauth-vuln/

## [v6.6.10-1] - 2023-08-18
### Changed
- Update CAS to 6.6.10 to fix a OpenID-Connect and OAuth vulnerability (#182)
  - see https://apereo.github.io/2023/07/21/oidc-vuln/

## [v6.6.8-2] - 2023-06-26
### Added
- Config options for [resource requirements](https://github.com/cloudogu/dogu-development-docs/blob/main/docs/important/relevant_functionalities_en.md#resource-requirements) (#180)

## [v6.6.8-1] - 2023-06-16
### Changed
- Update CAS to 6.6.8 (#178)
- Update Tomcat to 9.0.85

### Removed
- Remove /var/lib/cas Volume

## [v6.5.9-1] - 2023-05-12
### Changed
- Upgrade cas to 6.5.9.1 (#175)

### Security
- spring-framework: CVE-2023-20861
- sprint-boot: CVE-2022-22965 / CVE-2023-20873 / CVE-2022-22965 / GHSA-36p3-wjmg-h94x / CVE-2023-20873
- snakeyaml: CVE-2022-25857 / CVE-2022-38749 / CVE-2022-38749 / CVE-2022-38749 /  CVE-2022-38752 / CVE-2022-41854 / CVE-2022-1471
- commons-text: CVE-2022-42889
- netty: CVE-2019-20444 / CVE-2019-20445 / CVE-2019-16869 / CVE-2021-21290 / CVE-2021-21409 / CVE-2021-43797 / CVE-2022-24823
- jackson.core: CVE-2020-36518 / CVE-2020-36518 / CVE-2022-42004 / CVE-2022-42003
- junit: CVE-2020-15250
- smart-json: CVE-2023-1370
- jose4j: GHSA-jgvc-jfgh-rjvv
- json: CVE-2022-45688
- jsoup: CVE-2022-36033

## [v6.5.8-2] - 2023-03-22
### Fixed
- Fix file system exhaustion from Tomcat access logs in `/opt/apache-tomcat/logs` (#173)
  - The access logs will be streamed to Stdout instead, t. i., the logs will be accommodated by the hosts `/var/lib/docker/cas.log`

## [v6.5.8-1] - 2022-11-17
- Upgrade cas to 6.5.8 (#171)

## [v6.5.5-4] - 2022-08-23
### Changed
- Set the `ldap-min-pool-size` to zero also for the password management ldap (#136, #169)

## [v6.5.5-3] - 2022-08-17
### Added
- Make password policy configurable. For more information see [docs](docs/operations/password-management_en.md#Configuration-of-password-rules-in-etcd) (#167)

## [v6.5.5-2] - 2022-07-07
### Changed
- When resetting the password, certain e-mail addresses are declared invalid in the original CAS code, e.g.
  `admin@ces.local`. This has now been adjusted. E-mails are now sent to all e-mail addresses (concretely: forwarded to
  Postfix). (#163)

## [v6.5.5-1] - 2022-06-30
### Changed
- Update cas to v6.5.5 (#164)

## [v6.5.5-1] - 2022-06-30
### Changed
- Update cas to v6.5.5 (#164)

## [v6.5.3-8] - 2022-05-25
### Changed
- Suppress determination of an existing username via password reset function (#161)
  - Previously, an error message has been displayed if a username does not exist in the system. If the username is
  present in the system, a confirmation that an email has been sent followed. Now a confirmation page with customised
  text is displayed in both cases.

## [v6.5.3-7] - 2022-05-18
### Fixed
- If CAS version 6.3.7-5, 6.5.2-1 or 6.5.3-1 has been used and an upgrade to a version >= 6.5.3.2 has been carried out,
the migration of the service account for the LDAP from the read account to the write account is not performed. This
resulted in a password change not being saved by the user and the user receiving an error message. This error has now
been corrected. (#159)

## [v6.5.3-6] - 2022-05-11
### Added
- Password Reset Functionality. For more information see [docs](docs/operations/password-management_en.md) (#156)

### Fixed
- Forgotten password button has always been displayed. If no text has been defined in etcd, a useless default text has
been displayed. (#157)

## [v6.5.3-5] - 2022-04-29
### Fixed
- Fix wrong translation on password reset view (#154)
- Change reset password view for better ui flow

## [v6.5.3-4] - 2022-04-28
### Changed
- Enhance forgot password feature, enhance accessibility (#152)

## [v6.5.3-3] - 2022-04-27
### Changed
- fix proxy ticket validation with services contain ports (#150)

## [v6.5.3-2] - 2022-04-27
- Activate password policy to allow changing password after first login (#145)

## [v6.5.3-1] - 2022-04-26
### Changed
- Upgrade cas to 6.5.3 (#147)

## [v6.5.2-1] - 2022-04-13
### Changed
- Upgrade cas war overlay version to 6.5.2 (#141)
- Update java base image to 11.0.14-3 (#141)
- Update all base image packages prior to building the cas app (#141)
- Upgrade spring boot to version 2.6.6 (#141)
- Upgrade cypress to version 9.5.4 for the integration tests (#141)

### Fixed
- Fixed german translation on login page (#138).

## [v6.3.7-5] - 2022-04-11
### Changed
- Set min-width for notch to fully display floating label for username (#143)

## [v6.3.7-4] - 2022-03-29
### Changed
- Update java base image to 11.0.14-2 (#139)

## [v6.3.7-3] - 2022-02-02
### Changed
- Set the `ldap-min-pool-size` to zero (#136)

## [v6.3.7-2] - 2022-01-11
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
