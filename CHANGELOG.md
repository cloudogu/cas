# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [unreleased]

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

Under the hood we verify the Tomcat binary to spot (possibly) tampered Tomcat binaries during the build time. (#38)

### Fixed

PerformanceStats are no longer logged to the container filesystem for reasons of discoverability and performance. Instead they are logged to the usual CES logging facility. (#48)

## [v4.0.7.20-7](https://github.com/cloudogu/cas/releases/tag/v4.0.7.20-7) - 2020-03-12 

### Added
* cas config etcd key `session_tgt/max_time_to_live_in_seconds` to configure maximum session timeout
* cas config etcd key `session_tgt/time_to_kill_in_seconds` to configure idle session timeout
