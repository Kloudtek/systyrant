/*
 * Copyright (c) 2015. Kelewan Technologies Ltd
 */

package com.kloudtek.kloudmake.exception;

import org.slf4j.Logger;

public class InjectException extends KMRuntimeException {
    public InjectException() {
    }

    public InjectException(String message) {
        super(message);
    }

    public InjectException(Logger logger, String message) {
        super(logger, message);
    }

    public InjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public InjectException(Logger logger, String message, Throwable cause) {
        super(logger, message, cause);
    }

    public InjectException(Throwable cause) {
        super(cause);
    }

    public InjectException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
