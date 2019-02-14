package de.triology.cas.limiting;

import javax.security.auth.login.AccountException;

public class AccountTemporarilyLockedException extends AccountException {
    public AccountTemporarilyLockedException() {
        super("account temporarily locked due to too many login failures");
    }
}
