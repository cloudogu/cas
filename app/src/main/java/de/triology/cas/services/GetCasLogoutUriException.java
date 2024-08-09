package de.triology.cas.services;

public class GetCasLogoutUriException extends Exception {
    GetCasLogoutUriException(String message) {
        super(message);
    }

    GetCasLogoutUriException(Exception cause) {
        super(cause);
    }
}
