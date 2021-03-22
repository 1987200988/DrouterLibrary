/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.store;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import com.didi.drouter.annotation.Cache;
import com.didi.drouter.api.RouterType;
import com.didi.drouter.interceptor.IInterceptor;
import com.didi.drouter.router.IRouterHandler;
import com.didi.drouter.utils.ReflectUtil;
import com.didi.drouter.utils.RouterLogger;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2018/8/31
 */
@SuppressWarnings({"unchecked", "SpellCheckingInspection"})
public class RouterStore {

    public static final String HOST = "host";
    static final String REGEX_ROUTER = "RegexRouter";

    private static final Map<String, Object> routerMetas = new ConcurrentHashMap<>();      //key is uriKey，value is meta or map, with dynamic
    private static final Map<Class<? extends IInterceptor>, RouterMeta> interceptorMetas = new ArrayMap<>();      //key is interceptor impl
    private static final Map<Class<?>, Set<RouterMeta>> serviceMetas = new ArrayMap<>();   //key is interface，value is set, no dynamic
    private static final Map<Class<?>, Set<RouterMeta>> dynamicServiceMetas = new ConcurrentHashMap<>();   //dynamic

    private static final RouterLogger logger = RouterLogger.getCoreLogger();
    private static final Set<String> loadRecord = Collections.synchronizedSet(new ArraySet<String>());
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static boolean initialized;

    /**
     * support VirtualApk
     *
     * @param packageName host or use {pluginName} configuration in plugin apk gradle file
     */
    public static void load(String packageName) {
        if (!loadRecord.contains(packageName)) {
            synchronized (RouterStore.class) {
                if (!loadRecord.contains(packageName)) {
                    loadRecord.add(packageName);
                    logger.d("===DRouter load start in %s===", Thread.currentThread().getName());
                    long time = System.currentTimeMillis();

                    load("Router", routerMetas, packageName);
                    load("Interceptor", interceptorMetas, packageName);
                    load("Service", serviceMetas, packageName);

                    logger.d("===DRouter load complete=== waste time: %sms", System.currentTimeMillis() - time);
                    if (HOST.equals(packageName)) {
                        Statistics.init();
                        initialized = true;
                        latch.countDown();
                    }
                }
            }
        }
    }

    private static void load(String type, Map<?, ?> store, String packageName) {
        String target = String.format("com.didi.drouter.loader.%s.%sLoader", packageName, type);
        try {
            Class<?> clz = Class.forName(target);
            MetaLoader loader = (MetaLoader) ReflectUtil.getInstance(clz);
            if (loader != null) {
                loader.load(store);
                logger.d("%sLoader in %s load success", type, packageName);
            }
        } catch (ClassNotFoundException e) {
            logger.e("%sLoader in %s not found", type, packageName);
        }
    }

    @NonNull
    public static Set<RouterMeta> getRouterMetas(@NonNull Uri uriKey) {
        check();
        Set<RouterMeta> result = new ArraySet<>();
        Object o = routerMetas.get(uriKey.toString());
        if (o instanceof RouterMeta) {
            result.add((RouterMeta) o);
        }
        Map<String, RouterMeta> regex = (Map<String, RouterMeta>) routerMetas.get(REGEX_ROUTER);
        if (regex != null) {
            for (RouterMeta meta : regex.values()) {
                if (meta.isRegexMatch(uriKey)) {
                    result.add(meta);
                }
            }
        }
        return result;
    }

    @NonNull
    public static Map<Class<? extends IInterceptor>, RouterMeta> getInterceptors() {
        check();
        return interceptorMetas;
    }

    @NonNull
    public static Set<RouterMeta> getServiceMetas(Class<?> interfaceClass) {
        check();
        Set<RouterMeta> metas = serviceMetas.get(interfaceClass);
        if (metas == null) {
            return Collections.emptySet();
        }
        return metas;
    }

    @NonNull
    public static Set<RouterMeta> getDynamicServiceMetas(Class<?> interfaceClass) {
        check();
        Set<RouterMeta> metas = dynamicServiceMetas.get(interfaceClass);
        if (metas == null) {
            return Collections.emptySet();
        }
        return metas;
    }

    @NonNull
    public synchronized static IRegister register(final RouterKey key, final IRouterHandler handler) {
        if (key == null || handler == null) {
            throw new IllegalArgumentException("argument null illegal error");
        }
        check();
        boolean success = false;
        RouterMeta meta = RouterMeta.build(RouterType.HANDLER).assembleRouter(
                key.uri.getScheme(), key.uri.getHost(), key.uri.getPath(),
                (Class<?>) null, key.interceptor, key.thread, key.waiting);
        meta.setHandler(key, handler);
        if (meta.isRegexUri()) {
            Map<String, RouterMeta> regexMap = (Map<String, RouterMeta>) routerMetas.get(REGEX_ROUTER);
            if (regexMap == null) {
                regexMap = new ConcurrentHashMap<>();
                routerMetas.put(REGEX_ROUTER, regexMap);
            }
            if (!regexMap.containsKey(meta.getLegalUri())) {
                success = true;
                regexMap.put(meta.getLegalUri(), meta);
            }
        } else {
            if (!routerMetas.containsKey(meta.getLegalUri())) {
                success = true;
                routerMetas.put(meta.getLegalUri(), meta);
            }
        }
        if (success) {
            if (key.lifecycleOwner != null && key.lifecycleOwner.getLifecycle() != null) {
                key.lifecycleOwner.getLifecycle().addObserver(new GenericLifecycleObserver() {
                    @Override
                    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                        if (event == Lifecycle.Event.ON_DESTROY) {
                            unregister(key, handler);
                        }
                    }
                });
            }
            RouterLogger.getCoreLogger().d("register \"%s\" with handler \"%s\" success",
                    meta.getLegalUri(), handler.getClass().getSimpleName());
            return new RouterRegister(key, handler, true);
        }
        return new RouterRegister(key, handler, false);
    }

    synchronized static void unregister(RouterKey key, IRouterHandler handler) {
        if (key != null && handler != null) {
            RouterMeta meta = RouterMeta.build(RouterType.HANDLER).assembleRouter(
                    key.uri.getScheme(), key.uri.getHost(), key.uri.getPath(),
                    (Class<?>) null, key.interceptor, key.thread, key.waiting);
            boolean success = false;
            if (meta.isRegexUri()) {
                Map<String, RouterMeta> regexMap = (Map<String, RouterMeta>) routerMetas.get(REGEX_ROUTER);
                if (regexMap != null) {
                    success = regexMap.remove(meta.getLegalUri()) != null;
                }
            } else {
                success = routerMetas.remove(meta.getLegalUri()) != null;
            }
            if (success) {
                RouterLogger.getCoreLogger().d("unregister \"%s\" with handler \"%s\" success",
                        meta.getLegalUri(), handler.getClass().getSimpleName());
            }
        }
    }

    @NonNull
    public synchronized static <T> IRegister register(final ServiceKey<T> key, final T service) {
        if (key == null || key.function == null || service == null) {
            throw new IllegalArgumentException("argument null illegal error");
        }
        RouterMeta meta = RouterMeta.build(RouterType.SERVICE).assembleService(
                null, key.alias, key.feature, 0, Cache.NO);
        meta.setService(key, service, key.unregisterAfterExecute);
        Set<RouterMeta> metas = dynamicServiceMetas.get(key.function);
        if (metas == null) {
            metas = Collections.newSetFromMap(new ConcurrentHashMap<RouterMeta, Boolean>());
            dynamicServiceMetas.put(key.function, metas);
        }
        if (key.clearPrevious) {
            metas.clear();
        } else {
            for (RouterMeta iter : metas) {
                if (iter.getService() == service) {
                    return new RouterRegister(key, service, false);
                }
            }
        }
        metas.add(meta);
        if (key.lifecycleOwner != null && key.lifecycleOwner.getLifecycle() != null) {
            key.lifecycleOwner.getLifecycle().addObserver(new GenericLifecycleObserver() {
                @Override
                public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        unregister(key, service);
                    }
                }
            });
        }
        RouterLogger.getCoreLogger().d("register \"%s\" with service \"%s\" success, size:%s",
                key.function.getSimpleName(), service, metas.size());
        return new RouterRegister(key, service, true);
    }

    public synchronized static void unregister(ServiceKey key, Object service) {
        if (key != null && service != null) {
            Set<RouterMeta> metas = dynamicServiceMetas.get(key.function);
            if (metas != null) {
                Iterator<RouterMeta> iterator = metas.iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().getService() == service) {
                        iterator.remove();
                        RouterLogger.getCoreLogger().d("unregister \"%s\" with service \"%s\" success",
                                key.function.getSimpleName(), service);
                    }
                }
            }
        }
    }

    private static void check() {
        if (!initialized) {
            RouterStore.load(HOST);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}