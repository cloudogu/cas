package de.triology.cas.ldap;

public class CesLdapException extends Exception {
    public CesLdapException(String msg) {
        super(msg);
    }

    public CesLdapException(String msg, Throwable e) {
        super(msg, e);
    }
}
