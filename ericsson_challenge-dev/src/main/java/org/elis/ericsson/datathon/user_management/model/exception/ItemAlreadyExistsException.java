package org.elis.ericsson.datathon.user_management.model.exception;

public class ItemAlreadyExistsException extends RuntimeException {

    public ItemAlreadyExistsException(String message) {
        super(String.format(message));
    }
}
