# Release Notes

Below you will find the release notes for CAS-Dogu.

Technical details on a release can be found in the corresponding [Changelog](https://docs.cloudogu.com/de/docs/dogus/cas/CHANGELOG/).

## [Unreleased]

## [v7.1.6-6] - 2025-08-14
### Fixed
- Fixed an issue where the Dogu could not apply a specific configuration.

## [v7.0.10-5] - 2025-08-14
### Fixed
- Fixed an issue where the Dogu could not apply a specific configuration.

## [v7.1.6-5] - 2025-08-13
### Fixed
- Remove expired sessions that stay active by configuring an interval-based registry cleaner
  -  The interval can be configured with the `registry_cleaner/repeat-interval`-Key in seconds (default-value: 2)

## [v7.0.10-4] - 2025-08-13
### Fixed
- Remove expired sessions that stay active by configuring an interval-based registry cleaner
  - The interval can be configured with the `registry_cleaner/repeat-interval`-Key in seconds (default-value: 2)


## [v7.1.6-4] - 2025-07-28
### Fixed
- The LogoutURL contains the correct FQDN if it is changed via the global configuration.

## [v7.1.6-3] - 2025-05-09
### Fixed
- Fix CAS 3 success view for Redmine missing formattedAttributes due to overwritten view and incomplete model
- Fix destroying the oidc-session on logout
    - When the session was not destroyed on logout the user-profile was cached and the user was not updated in the OIDC-session.
    - This caused that possible changes of the user (like username or group assignments) were not updated

## [v7.0.10-3] - 2025-05-09
### Fixed
- Fix destroying the oidc-session on logout
    - When the session was not destroyed on logout the user-profile was cached and the user was not updated in the OIDC-session.
    - This caused that possible changes of the user (like username or group assignments) were not updated

## [v7.1.6-2] - 2025-05-08
### Fixed
- Restored support for proxy ticket authentication, allowing Dogus like SCM to authenticate via Smeagol using CAS.

## [v7.1.6-1] - 2025-04-30
### Changed
- [#263] Upgrade CAS to version 7.1.6

## [v7.0.10-2] - 2025-04-23
### Changed
- Usage of memory and CPU was optimized for the Kubernetes Mutlinode environment. 

## [v7.0.10-1] - 2025-04-17
### Changed
- [#261] Upgrade CAS to version 7.0.10.1

## [v7.0.8-14] - 2025-04-16
### Security
- [Fix CVE-2024-38821](https://nvd.nist.gov/vuln/detail/CVE-2024-38821) – Spring Security Authorization Bypass in WebFlux
- [Fix CVE-2025-24813](https://nvd.nist.gov/vuln/detail/CVE-2025-24813) – Apache Tomcat: HTTP/2 stream handling vulnerability
- [Fix OIDC WebAuthn Vulnerability](https://apereo.github.io/2025/04/11/oidc-webauthn-vuln/) –   OIDC-Backchannel Authentication with WebAuthn could allow privilege escalation under specific circumstances  

## [v7.0.8-13] - 2025-04-07
- Makes the CAS start-up more robust against unset or empty configuration keys.

## [v7.0.8-12] - 2025-03-12
- Cleanup old service-accounts from JSON-registry before creating a new service-account

## Release 7.0.8-11
- Makes the CAS start-up more robust against unset configuration keys.

## Release 7.0.8-10
- When logging in via delegated authentication, `allowed_groups` and `initial_admin_usernames` can now be configured.
  - `allowed_groups`: Specifies a list of OIDC groups that are allowed to log on with delegated authentication. The groups are separated by commas. An empty list allows access for all.
  - `initial_admin_usernames`: Specifies a list of usernames that are assigned to the CES admin group at the first login.

## Release 7.0.8-9
- Fixed a problem where the Dogu does not start under high system load.

## Release 7.0.8-8
- The button for revealing the password has been converted into a checkbox for better operation.

## Release 7.0.8-7
- Fixed a technical bug where the upgrade process was interrupted
- Fixed a technical bug where the cockpit dogu could not be called under certain conditions

## Release 7.0.8-6
- Invalid login data are no longer logged

## Release 7.0.8-5
- The Dogu has been internally converted to a JSON registry, which has changed the logic for creating and deleting service accounts.
- Consistent use of service accounts in both multinode and singlenode environments.

### Breaking Change
- Newly installed dogus must explicitly request the creation of a service account in the CAS via dogu.json. Further information on this can be found in the [developer documentation](https://github.com/cloudogu/dogu-development-docs/blob/main/docs/important/relevant_functionalities_en.md#authentifizierung)

## Release 7.0.8-4
- When logging in via delegated authentication (using an OIDC provider), the users are replicated in the embedded LDAP
    - The replicated users are marked as “external” and cannot be edited, except for the group assignments.

## Release 7.0.8-3
- An adjustment has been made that extends compatibility for Dogus that use Open ID Connect.

## Release 7.0.8-2
- Resolved a technical issue in multinode environment, that caused that dogus with service accounts `cas` are not available.

## Release 7.0.8-1
- The Dogu now offers the CAS version 7.0.8. The release notes of CAS can be found [in the CAS Github releases](https://github.com/apereo/cas/releases/tag/v7.0.8).
- The CAS HTML pages now contain a “lang” attribute to increase accessibility.

## Release 7.0.5.1-8
- Relicense own code to AGPL-3.0-only.

## Release 7.0.5.1-7
- A Dogu upgrade now works better in Cloudogu EcoSystem multinode instances without etcd

## Release 7.0.5.1-6
- This release fixes bugs introduced in versions 7.0.5.1-4 and 7.0.5.1-5 that occurred during the Dogu upgrade

## Release 7.0.5.1-5
- Missing translations were added in this release.

## Release 7.0.5.1-4
- This release changes the way other Dogus service accounts create against the CAS to ensure smoother operation in Cloudogu EcoSystem multinode instances.
- Dogu states needed during startup are now held in volumes and no longer in etcd for security reasons.

## Release 7.0.5.1-3
- Fixes style issues in the login screen

## Release 7.0.5.1-2

- The design of the Dogus has been redesigned to match our new theme
    - This new design is completely whitelabelable in combination with the whitelabeling-Dogu and the new version of Nginx (>=v1.26.1-5).
  
## Release 7.0.5.1-1

- The Dogu now offers the CAS version 7.0.5.1. The release notes of CAS can be found [in the CAS Github releases](https://github.com/apereo/cas/releases/tag/v7.0.5.1).
- The Dogu offers a functionality for blocking repeated incorrect logins over a defined period of time (throttling).
- In the past, especially with the introduction of CAS 7.x, a faulty standard configuration in connection with Dogu-internal users led to blocking of the entire Cloudogu EcoSystem. This bug has been fixed in this version. Some Dogu configuration keys have been replaced by new configuration keys:
   - no longer supported:
     - `limit/max_numbers`
     - `limit/failure_store_time`
   - `limit/failure_threshold` now configures the maximum number of failed login attempts in a time period, which `limit/range_seconds` configures in seconds
   - `limit/stale_removal_interval` configures the time a cleanup job waits in the background until it restarts to search for obsolete throttling entries

## Release 7.0.4.1-2

- The dogu now no longer log passwords in the debug log level
- The Dogu offers a security fix for the CAS dogu version 7.0.4.1-1. The Redmine release notes can be found [in the CAS Github releases](https://github.com/apereo/cas/releases/tag/v7.0.4.1).
- In the past, passwords were split into individual characters in the CAS logs after switching to the debug log level. This bug has been fixed in this version.
