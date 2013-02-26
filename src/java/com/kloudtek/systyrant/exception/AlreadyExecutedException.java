/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.systyrant.exception;

public class AlreadyExecutedException extends RuntimeException {
    public AlreadyExecutedException() {
    }

    public AlreadyExecutedException(String message) {
        super(message);
    }

    public AlreadyExecutedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyExecutedException(Throwable cause) {
        super(cause);
    }

    public AlreadyExecutedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
