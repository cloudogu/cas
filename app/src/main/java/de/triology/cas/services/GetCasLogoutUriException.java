package de.triology.cas.services;

class GetCasLogoutUriException extends Exception {
    GetCasLogoutUriException(String message) {
        super(message);
    }

    GetCasLogoutUriException(Exception cause) {
        super(cause);
    }
}
