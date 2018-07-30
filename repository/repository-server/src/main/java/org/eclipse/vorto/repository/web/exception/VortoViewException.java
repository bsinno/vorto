package org.eclipse.vorto.repository.web.exception;

public class VortoViewException extends RuntimeException {
    private final int statusCode;
    private final int errorCode;

    public VortoViewException(int statusCode, int errorCode, String message, Throwable cause){
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public VortoViewException(int statusCode, int errorCode, String message){
        super(message, null);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
