/*
 * Copyright (c) 2013 KloudTek Ltd
 */

package com.kloudtek.systyrant.context.ioc;

import com.kloudtek.systyrant.Resource;
import com.kloudtek.systyrant.STContext;
import com.kloudtek.systyrant.ServiceManager;
import com.kloudtek.systyrant.annotation.Service;
import com.kloudtek.systyrant.exception.FieldInjectionException;
import com.kloudtek.systyrant.exception.InvalidServiceException;

import java.lang.reflect.Field;

import static com.kloudtek.util.StringUtils.isNotEmpty;

/**
 * This class is used to inject a service into a java resource implementation (see {@link Service} annotation for more
 * details of injection logic).
 */
public class ServiceInjector extends Injector {
    private final String name;

    public ServiceInjector(Class<?> clazz, Field field, Service service) {
        super(clazz, field);
        name = service.value();
    }

    @Override
    public void inject(Resource resource, Object obj, STContext ctx) throws FieldInjectionException {
        try {
            ServiceManager serviceManager = ctx.getServiceManager();
            boolean nameSpecified = isNotEmpty(name);
            Object service = nameSpecified ? serviceManager.getService(name) : serviceManager.getService(field.getType());
            if (service == null) {
                throw new FieldInjectionException(field, "No service " + (nameSpecified ? "named '" + name + "' found" :
                        "of type " + field.getType()) + " found");
            }
            inject(obj, service);
        } catch (InvalidServiceException e) {
            throw new FieldInjectionException(field, e);
        }
    }
}