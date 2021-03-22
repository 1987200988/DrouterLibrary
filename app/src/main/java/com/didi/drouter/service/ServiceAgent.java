/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.service;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.didi.drouter.annotation.Cache;
import com.didi.drouter.api.RouterType;
import com.didi.drouter.remote.RemoteBridge;
import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.store.Statistics;
import com.didi.drouter.utils.ReflectUtil;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2019/4/1
 */
@SuppressWarnings("unchecked")
class ServiceAgent<T> {

    // cache, key is impl
    private static Map<Class<?>, Object> instanceMap = new ConcurrentHashMap<>();
    private static Map<Class<?>, WeakReference<Object>> weakInstanceMap = new ConcurrentHashMap<>();

    // key is function
    private static Map<Class<?>, Map<Class<?>, RouterMeta>> serviceImplMap = new ConcurrentHashMap<>();
    private Class<T> function;
    private @NonNull
    String alias = "";
    private Object feature;
    private String authority;
    private boolean resend;
    private WeakReference<LifecycleOwner> lifecycle;

    ServiceAgent(Class<T> function) {
        Statistics.track("service");
        this.function = function;
        Map<Class<?>, RouterMeta> implMap = serviceImplMap.get(function);
        if (implMap == null) {
            synchronized (ServiceLoader.class) {
                implMap = serviceImplMap.get(function);
                if (implMap == null) {
                    implMap = new ArrayMap<>();
                    Set<RouterMeta> metaSet = RouterStore.getServiceMetas(function);
                    for (RouterMeta meta : metaSet) {
                        if (meta.getRouterType() == RouterType.SERVICE && meta.getTargetClass() != null) {
                            implMap.put(meta.getTargetClass(), meta);
                        }
                    }
                    serviceImplMap.put(function, implMap);
                }
            }
        }
    }

    void setAlias(String alias) {
        this.alias = alias != null ? alias : "";
    }

    void setFeature(Object feature) {
        this.feature = feature;
    }

    void setRemoteAuthority(String authority) {
        this.authority = authority;
    }

    void setRemoteResend(boolean resend) {
        this.resend = resend;
    }

    void setLifecycleOwner(LifecycleOwner owner) {
        this.lifecycle = owner != null ? new WeakReference<>(owner) : null;
    }

    Class<? extends T> getServiceClass() {
        List<Class<? extends T>> result = getAllServiceClass();
        if (!result.isEmpty()) {
            Class<? extends T> target = result.get(0);
            RouterLogger.getCoreLogger().d("[..] Get service \"%s\" class for function \"%s\"", target.getSimpleName(), function.getSimpleName());
            return target;
        }
        return null;
    }

    @NonNull
    List<Class<? extends T>> getAllServiceClass() {
        List<Class<? extends T>> result = new ArrayList<>();
        if (function != null) {
            for (Map.Entry<Class<?>, RouterMeta> entry : serviceImplMap.get(function).entrySet()) {
                if (match(entry.getValue().getServiceAlias(), entry.getValue().getFeatureMatcher())) {
                    result.add((Class<? extends T>) entry.getKey());
                }
            }
            if (result.size() > 1) {
                Collections.sort(result, new ServiceComparator());
            }
            if (result.isEmpty()) {
                RouterLogger.getCoreLogger().e("function \"%s\" no service match", function.getSimpleName());
            }
        }
        return result;
    }

    T getService(Object... constructors) {
        // remote
        if (!TextUtils.isEmpty(authority)) {
            RouterLogger.getCoreLogger().d("[..] Get remote service \"%s\" by proxy", function.getSimpleName());
            return RemoteBridge.load(authority, resend, lifecycle).getService(function, alias, feature, constructors);
        }
        // normal
        Object target = getServiceInstance(getServiceClass(), constructors);
        // ICallServiceX
        if (function == ICallService.class && target != null && !(target instanceof ICallService)) {
            return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {function}, new CallHandler(target));
        }
        // dynamic register
        if (target == null) {
            for (RouterMeta meta : RouterStore.getDynamicServiceMetas(function)) {
                if (match(meta.getServiceAlias(), meta.getFeatureMatcher())) {
                    return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {function}, new DynamicHandler(meta));
                }
            }
        }
        return (T) target;
    }

    @NonNull
    List<T> getAllService(Object... constructors) {
        List<T> result = new ArrayList<>();
        if (function != null) {
            // normal
            for (Class<? extends T> implClass : getAllServiceClass()) {
                T t = (T) getServiceInstance(implClass, constructors);
                if (t != null) {
                    result.add(t);
                }
            }
            // dynamic
            for (RouterMeta meta : RouterStore.getDynamicServiceMetas(function)) {
                if (match(meta.getServiceAlias(), meta.getFeatureMatcher())) {
                    result.add((T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {function}, new DynamicHandler(meta)));
                }
            }
        }
        return result;
    }

    private boolean match(String alias, IFeatureMatcher feature) {
        return this.alias.equals(alias) && (feature == null || feature.match(this.feature));
    }

    private static class CallHandler implements InvocationHandler {

        Object callService;

        CallHandler(Object callService) {
            this.callService = callService;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object[] params = (Object[]) args[0];
            if (params == null) {
                params = new Object[] {null};
            }
            if (callService instanceof ICallService0 && params.length == 0) {
                return ((ICallService0) callService).call();
            }
            if (callService instanceof ICallService1 && params.length == 1) {
                return ((ICallService1) callService).call(params[0]);
            }
            if (callService instanceof ICallService2 && params.length == 2) {
                return ((ICallService2) callService).call(params[0], params[1]);
            }
            if (callService instanceof ICallService3 && params.length == 3) {
                return ((ICallService3) callService).call(params[0], params[1], params[2]);
            }
            if (callService instanceof ICallService4 && params.length == 4) {
                return ((ICallService4) callService).call(params[0], params[1], params[2], params[3]);
            }
            if (callService instanceof ICallService5 && params.length == 5) {
                return ((ICallService5) callService).call(params[0], params[1], params[2], params[3], params[4]);
            }
            if (callService instanceof ICallServiceN) {
                return ((ICallServiceN) callService).call(params);
            }
            RouterLogger.getCoreLogger().e("%s not match with argument length %s ", callService.getClass().getSimpleName(), params.length);
            return null;
        }
    }

    private static class DynamicHandler implements InvocationHandler {

        RouterMeta serviceMeta;

        DynamicHandler(RouterMeta serviceMeta) {
            this.serviceMeta = serviceMeta;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (serviceMeta.isUnregisterAfterExecute()) {
                RouterStore.unregister(serviceMeta.getServiceKey(), serviceMeta.getService());
            }
            return method.invoke(serviceMeta.getService(), args);
        }
    }

    private @Nullable
    Object getServiceInstance(Class<?> implClass, Object... parameter) {
        if (implClass == null) return null;
        Object t = instanceMap.get(implClass);
        if (t == null && weakInstanceMap.containsKey(implClass)) {
            t = weakInstanceMap.get(implClass).get();
        }
        if (t == null) {
            synchronized (ServiceLoader.class) {
                t = instanceMap.get(implClass);
                if (t == null && weakInstanceMap.containsKey(implClass)) {
                    t = weakInstanceMap.get(implClass).get();
                }
                if (t == null) {
                    t = ReflectUtil.getInstance(implClass, parameter);
                    if (t != null) {
                        RouterLogger.getCoreLogger().d("[..] Get service \"%s\" instance by create new", t.getClass().getSimpleName());
                        if (serviceImplMap.get(function).get(implClass).getCache() == Cache.SINGLETON) {
                            instanceMap.put(implClass, t);
                        } else if (serviceImplMap.get(function).get(implClass).getCache() == Cache.WEAK) {
                            weakInstanceMap.put(implClass, new WeakReference(t));
                        }
                        return t;
                    }
                }
            }
        }
        if (t != null) {
            RouterLogger.getCoreLogger().d("[..] Get service \"%s\" instance by cache", t.getClass().getSimpleName());
        }
        return t;
    }

    // from large to small
    private class ServiceComparator implements Comparator<Class<?>> {
        @Override
        public int compare(Class<?> o1, Class<?> o2) {
            Map<Class<?>, RouterMeta> metaMap = serviceImplMap.get(function);
            int priority1 = metaMap.get(o1).getPriority();
            int priority2 = metaMap.get(o2).getPriority();
            return priority2 - priority1;
        }
    }
}
