/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.systyrant.resource;

import com.kloudtek.systyrant.FQName;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ResourceMatcher {
    private String pkg;
    private String name;

    public ResourceMatcher(@NotNull String pkg, String name) {
        this.pkg = pkg;
        this.name = name;
    }

    public ResourceMatcher(FQName fqName) {
        this.pkg = fqName.getPkg();
        this.name = fqName.getName();
    }

    public String getPkg() {
        return pkg;
    }

    public String getName() {
        return name;
    }

    public boolean matches(FQName fqname) {
        return fqname.getPkg().equals(pkg) && (name == null || fqname.getName().equals(name));
    }

    public static boolean matchAll(Collection<ResourceMatcher> importPaths, FQName fqName) {
        if (importPaths == null || fqName.getPkg().equals("default")) {
            return true;
        } else {
            for (ResourceMatcher importPath : importPaths) {
                if (importPath.matches(fqName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
