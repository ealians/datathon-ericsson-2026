package org.elis.ericsson.datathon.user_management.model.exception;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(String message) {
        super(String.format(message));
    }
    public ItemNotFoundException(Throwable cause) {
        super(cause);
    }
    public ItemNotFoundException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
