# Release Notes

Below you will find the release notes for CAS-Dogu.

Technical details on a release can be found in the corresponding [Changelog](https://docs.cloudogu.com/de/docs/dogus/cas/CHANGELOG/).

## Release 7.0.8-3
An adjustment has been made that extends compatibility for Dogus that use Open ID Connect.

## Release 7.0.8-2
Resolved a technical issue in multinode environment, that caused that dogus with service accounts `cas` are not available.

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
