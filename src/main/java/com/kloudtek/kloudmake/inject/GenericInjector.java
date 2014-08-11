/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.kloudmake.inject;

import com.kloudtek.kloudmake.KMContextImpl;
import com.kloudtek.kloudmake.Resource;
import com.kloudtek.kloudmake.ServiceManager;
import com.kloudtek.kloudmake.exception.FieldInjectionException;
import com.kloudtek.kloudmake.exception.InvalidAttributeException;
import com.kloudtek.kloudmake.host.Host;

import java.lang.reflect.Field;

/**
 * Injector for the {@link com.kloudtek.kloudmake.annotation.Inject} annotation.
 */
public class GenericInjector extends AttrInjector {
    private Type type;
    private final Class<?> fieldType;

    public GenericInjector(Class<?> clazz, Field field) {
        super(clazz, field.getName(), field);
        fieldType = field.getType();
        if (KMContextImpl.class.isAssignableFrom(fieldType)) {
            type = Type.CONTEXT;
        } else if (Resource.class.isAssignableFrom(fieldType)) {
            type = Type.RESOURCE;
        } else if (ServiceManager.class.isAssignableFrom(fieldType)) {
            type = Type.SERVICEMANAGER;
        } else if (Host.class.isAssignableFrom(fieldType)) {
            type = Type.HOST;
        } else {
            type = Type.ATTR;
        }
    }

    @Override
    public void inject(Resource resource, Object obj, KMContextImpl ctx) throws FieldInjectionException {
        switch (type) {
            case CONTEXT:
                inject(obj, ctx);
                break;
            case RESOURCE:
                inject(obj, resource);
                break;
            case SERVICEMANAGER:
                inject(obj, ctx.getServiceManager());
                break;
            case HOST:
                inject(obj, resource.getHost());
                break;
            case ATTR:
                super.inject(resource, obj, ctx);
                break;
            default:
                throw new FieldInjectionException(field, "BUG! Unknown type " + type);
        }
    }

    @Override
    public void updateAttr(Resource resource, Object obj) throws IllegalAccessException, InvalidAttributeException {
        if (type == Type.ATTR) {
            super.updateAttr(resource, obj);
        }
    }

    public enum Type {
        CONTEXT, RESOURCE, SERVICEMANAGER, HOST, SERVICE, ATTR
    }
}
