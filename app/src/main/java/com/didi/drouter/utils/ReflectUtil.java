/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.utils;


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Created by gaowei on 2018/9/4
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ReflectUtil {

    public static Class getClass(@NonNull String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            RouterLogger.getCoreLogger().e("ReflectUtil \"%s\" getClass exception %s", className, e);
        }
        return null;
    }

    public static Object getInstance(@NonNull String className, Object... params) {
        try {
            return getInstance(Class.forName(className), params);
        } catch (ClassNotFoundException e) {
            RouterLogger.getCoreLogger().e("ReflectUtil \"%s\" getInstance exception %s", className, e);
        }
        return null;
    }

    // not support non static inner class
    public static Object getInstance(@NonNull Class<?> implClass, @Nullable Object... params) {
        try {
            // null indicates a null param
            if (params == null) {
                params = new Object[] {null};
            }
            if (params.length == 0) {
                return implClass.newInstance();
            }
            Constructor<?>[] constructors = implClass.getConstructors();
            if (constructors != null) {
                for (Constructor<?> constructor : constructors) {
                    Class<?>[] classes = constructor.getParameterTypes();
                    if (isParameterTypeMatch(classes, params)) {
                        return constructor.newInstance(params);
                    }
                }
            }
            RouterLogger.getCoreLogger().e("ReflectUtil \"%s\" getInstance no match and return \"null\"", implClass);
        } catch (Exception e) {
            RouterLogger.getCoreLogger().e("ReflectUtil \"%s\" getInstance Exception: %s", implClass, e);
        }
        return null;
    }

    public static Object invokeMethod(Object instance, String methodName, @Nullable Object[] params) throws Exception {
        if (params == null || params.length == 0) {
            Method method = instance.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(instance);
        }
        Method[] methods = instance.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                Class<?>[] classes = method.getParameterTypes();
                if (isParameterTypeMatch(classes, params)) {
                    method.setAccessible(true);
                    return method.invoke(instance, params);
                }
            }
        }
        throw new Exception("ReflectUtil invokeMethod no match");
    }

    private static boolean isParameterTypeMatch(@NonNull Class[] target, @NonNull Object[] params) {
        if (target.length != params.length) return false;
        for (int i = 0; i < target.length; i++) {
            Class<?> host = target[i];
            Class<?> client = params[i] != null ? params[i].getClass() : null;
            boolean typeMatch;
            if (client == null) {
                typeMatch = !host.isPrimitive();
            } else {
                typeMatch = host == client ||
                        host.isAssignableFrom(client) ||
                        transform(host) == transform(client);
            }
            if (!typeMatch) {
                return false;
            }
        }
        return true;
    }

    private static Class transform(Class c) {
        if (c == Byte.class)
            c = byte.class;
        else if (c == Short.class)
            c = short.class;
        else if (c == Integer.class)
            c = int.class;
        else if (c == Long.class)
            c = long.class;
        else if (c == Float.class)
            c = float.class;
        else if (c == Double.class)
            c = double.class;
        else if (c == Character.class)
            c = char.class;
        else if (c == Boolean.class)
            c = boolean.class;
        return c;
    }


}