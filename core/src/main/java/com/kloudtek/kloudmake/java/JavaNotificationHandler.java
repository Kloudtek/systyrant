/*
 * Copyright (c) 2015. Kelewan Technologies Ltd
 */

package com.kloudtek.kloudmake.java;

import com.kloudtek.kloudmake.Notification;
import com.kloudtek.kloudmake.NotificationHandler;
import com.kloudtek.kloudmake.Resource;
import com.kloudtek.kloudmake.annotation.HandleNotification;
import com.kloudtek.kloudmake.exception.KMRuntimeException;
import com.kloudtek.kloudmake.util.ReflectionHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: yannick
 * Date: 31/03/13
 * Time: 00:54
 * To change this template use File | Settings | File Templates.
 */
public class JavaNotificationHandler extends NotificationHandler {
    private Method method;
    private Class<?> implClass;

    public JavaNotificationHandler(Method method, HandleNotification anno, Class<?> implClass) {
        super(anno.reorder(), anno.aggregate(), anno.onlyIfAfter(), anno.value());
        this.method = method;
        this.implClass = implClass;
    }

    @Override
    public void handleNotification(Notification notification) throws KMRuntimeException {
        Resource resource = notification.getTarget();
        try {
            method.invoke(resource.getJavaImpl(implClass));
        } catch (IllegalAccessException e) {
            throw new KMRuntimeException("Unable to invoke " + ReflectionHelper.toString(method));
        } catch (InvocationTargetException e) {
            throw new KMRuntimeException(e.getMessage(), e);
        }
    }
}
