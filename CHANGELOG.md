# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
