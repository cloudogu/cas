package de.triology.cas.services;

/**
 * Wraps exceptions thrown when accessing the {@link Registry}.
 */
class RegistryException extends RuntimeException {

    /**
     * Constructs a new runtime exception with the specified cause and a detail message of
     * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail message of
     * <tt>cause</tt>).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              (A <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
     */
    RegistryException(String message, Exception cause) {
        super(message, cause);
    }
}
