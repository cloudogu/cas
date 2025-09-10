# Operations

Login attempts can be limited by temporarily blocking user accounts after a given number of failed logins in a
given time. In case the account is blocked, login attempts are redirected to a appropriate page. For this it is
irrelevant, whether the account exists or not (that is even not existing accounts can be blocked).

<strong>Note: CAS calculates an error rate based on the failure_threshold and range_seconds. It then
<ins>looks at the last two</ins> failed logins and checks whether the time is below the error rate.

Example: Failure_threshold = 500; Range_seconds = 10; Error rate = 500/10 = 50

=> An error rate of 50 means that throttling occurs when two failed logins occur within 0.04 seconds: 500/10s -> 50/1s -> 5/0.1s -> 2/0.04s
</strong>

Configuration is done in the CAS module using the following parameters:

* `limit/failure_threshold` Max number of login retries per account. If failed attempts exceed this number in a given time
  specified with the parameters below, the users ip address is locked temporarily. Internally calculated down to an error rate (see example above).

  Setting this parameter to `0` disables this feature.
  For a value greater zero the other parameters have to be set appropriate.

`limit/range_seconds` Specifies the time period during which the failed login attempts are evaluated for throttling. Internally calculated down to an error rate (see example above).

The time is specified in seconds and must be greater than zero if the feature is enabled.

* `limit/lock_time` Time the ip address will be locked after exceeding the number of login attempts.
  
The time is specified in seconds and has to be greater than zero, if the feature is activated.

* `limit/stale_removal_interval` Time in seconds between background runs that find and remove expired and stale login failures (must be a positive number, only has an effect if `limit/failure_threshold` > 0; default value is 60 seconds)