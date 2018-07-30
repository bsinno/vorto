package org.eclipse.vorto.repository.web.exception;

public class FileLengthException extends VortoViewException {
    private static final int STATUS_CODE = 400;
    private static final int ERROR_CODE = 1;

    public FileLengthException() {
        super(STATUS_CODE, ERROR_CODE, null);
    }

    public FileLengthException(String message) {
        super(STATUS_CODE, ERROR_CODE, message);
    }

}
