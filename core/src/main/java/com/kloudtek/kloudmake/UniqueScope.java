/*
 * Copyright (c) 2015. Kelewan Technologies Ltd
 */

package com.kloudtek.kloudmake;

/**
 * Resource uniqueness scopes.
 * <p>GLOBAL indicates that a resource must be unique globally.</p>
 * <p>HOST indicates that a resource must be unique within the scope of a host.</p>
 */
public enum UniqueScope {
    GLOBAL, HOST
}
