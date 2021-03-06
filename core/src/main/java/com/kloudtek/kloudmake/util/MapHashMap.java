/*
 * Copyright (c) 2015. Kelewan Technologies Ltd
 */

package com.kloudtek.kloudmake.util;

import java.util.HashMap;

public class MapHashMap<X, Y, Z> extends AutoCreateHashMap<X, HashMap<Y, Z>> {
    @Override
    protected HashMap<Y, Z> create() {
        return new HashMap<>();
    }
}
