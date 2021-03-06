/*
 * Copyright (c) 2015. Kelewan Technologies Ltd
 */

package com.kloudtek.kloudmake;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * Library ClassLoader
 */
public class LibraryClassLoader extends URLClassLoader {
    public LibraryClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public LibraryClassLoader(URL[] urls) {
        super(urls);
    }

    public LibraryClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }
}
