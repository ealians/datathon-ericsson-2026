package org.elis.ericsson.datathon.user_management.model.exception;

public class RequestError extends RuntimeException {
    public RequestError(String message) {
        super(message);
    }

    public RequestError(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestError(Throwable cause) {
        super(cause);
    }
}
