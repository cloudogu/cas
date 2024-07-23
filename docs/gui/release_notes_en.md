# Release Notes

Below you will find the release notes for CAS-Dogu.

Technical details on a release can be found in the corresponding [Changelog](https://docs.cloudogu.com/de/docs/dogus/cas/CHANGELOG/).

## Release 7.0.5.1-1

- The Dogu now offers the CAS version 7.0.5.1. The release notes of CAS can be found [in the CAS Github releases](https://github.com/apereo/cas/releases/tag/v7.0.5.1).
- The Dogu offers a functionality for blocking repeated incorrect logins over a defined period of time (throttling).
- In the past, especially with the introduction of CAS 7.x, a faulty standard configuration in connection with Dogu-internal users led to blocking of the entire Cloudogu EcoSystem. This bug has been fixed in this version. Some Dogu configuration keys have been replaced by new configuration keys:
   - no longer supported: `limit/max_numbers`
   - `limit/failure_threshold` now configures the maximum number of failed login attempts in a time period, which `limit/range_seconds` configures in seconds
   - `limit/stale_removal_interval` configures the time a cleanup job waits in the background until it restarts to search for obsolete throttling entries

## Release 7.0.4.1-2

- The dogu now no longer log passwords in the debug log level
- The Dogu offers a security fix for the CAS dogu version 7.0.4.1-1. The Redmine release notes can be found [in the CAS Github releases](https://github.com/apereo/cas/releases/tag/v7.0.4.1).
- In the past, passwords were split into individual characters in the CAS logs after switching to the debug log level. This bug has been fixed in this version.
