/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.systyrant.dsl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InvalidScriptException extends Exception {
    private String location;
    private String token;

    public InvalidScriptException() {
    }

    public InvalidScriptException(String message, Exception cause) {
        super(message, cause);
    }

    public InvalidScriptException(@NotNull String message, @NotNull String location, @Nullable String token, @Nullable Exception cause) {
        super("[" + location + "]: " + message, cause);
        this.location = location;
        this.token = token;
    }


    public InvalidScriptException(String location, String token, Exception cause) {
        this("Invalid text found: " + token, location, token, cause);
    }

    public String getLocation() {
        return location;
    }

    public String getToken() {
        return token;
    }
}
