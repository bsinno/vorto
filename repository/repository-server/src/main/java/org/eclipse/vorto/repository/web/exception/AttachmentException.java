package org.eclipse.vorto.repository.web.exception;

public class AttachmentException extends VortoViewException {

    private static final int STATUS_CODE = 400;
    private static final int ERROR_CODE = 2;

    public AttachmentException() {
        super(STATUS_CODE, ERROR_CODE, null);
    }

    public AttachmentException(String message) {
        super(STATUS_CODE, ERROR_CODE, message);
    }

}
