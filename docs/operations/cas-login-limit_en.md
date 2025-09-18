# Operations

Login attempts can be limited by temporarily blocking user accounts after a given number of failed logins in a
given time. In case the account is blocked, login attempts are redirected to a appropriate page. For this it is
irrelevant, whether the account exists or not (that is even not existing accounts can be blocked).

<strong>Note: CAS calculates an error rate based on the `failure_threshold` and `range_seconds`. It then
<u>looks at the last two</u> failed logins and checks whether the time is below the error rate.
</strong>

Example configuration:

| `failure_threshold` | `range_seconds` | Nominal error rate        | effective throttling starting at   |
|---------------------|-----------------|---------------------------|------------------------------------|
| 500                 | 10              | 50 incorrect Req/1000 ms  | 2 incorrect Req. in 0,04 Sek       |
| 500                 | 100             | 5 incorrect Req/1000 ms   | 2 incorrect Req. in 0,4 Sek        |
| 500                 | 200             | 2,5 incorrect Req/1000 ms | 2 incorrect Req. in 0,8 Sek        |
| 100                 | 200             | 0,5 incorrect Req/1000 ms | 2 incorrect Req. in 4,0 Sek        |

Calculation:
An error rate of 50 means that throttling occurs when two incorrect logins (see above) occur within
0.04 seconds:
```
  500 Req. in 10 s
= 50 Req. in 1000 ms
= 50/50 Req. in 1000/50 ms
= 1 Req. in 20 ms
= 2 Req. in 40 ms
= 2 Req. in 0,04s
```

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