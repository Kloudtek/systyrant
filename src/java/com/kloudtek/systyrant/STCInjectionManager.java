/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.systyrant;

import com.kloudtek.systyrant.inject.Injector;

import java.util.List;
import java.util.WeakHashMap;

public class STCInjectionManager {
    private final WeakHashMap<Class<?>, List<Injector>> injectors = new WeakHashMap<>();

    public STCInjectionManager() {
    }

    public void register(Object obj) {

    }

    public void inject(Object obj) {

    }

    public void registerAndInject(Object obj) {

    }
}