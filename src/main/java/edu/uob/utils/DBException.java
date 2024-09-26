package edu.uob.utils;

import java.io.Serial;

public class DBException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1;
    private ErrorType error;
    public DBException(ErrorType error) {
        super(error.getMessage());
        this.error = error;
    }
    public DBException(ErrorType error, String message) {
        super(message);
        this.error = error;
    }
    public ErrorType getErrorType() { return this.error; }
}