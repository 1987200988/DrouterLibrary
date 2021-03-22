/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.interceptor;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.didi.drouter.annotation.Cache;
import com.didi.drouter.api.RouterType;
import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.utils.ReflectUtil;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;

/**
 * Created by gaowei on 2018/9/5
 */
class InterceptorLoader {

    private static Map<Class<? extends IInterceptor>, IInterceptor> instanceMap = new ArrayMap<>();
    private static Map<Class<? extends IInterceptor>, WeakReference<IInterceptor>> weakInstanceMap = new ArrayMap<>();
    private static Set<Class<? extends IInterceptor>> globalInterceptor = new ArraySet<>();

    static {
        for (Map.Entry<Class<? extends IInterceptor>, RouterMeta> entry : RouterStore.getInterceptors().entrySet()) {
            if (entry.getValue().isGlobal()) {
                globalInterceptor.add(entry.getKey());
            }
        }
    }

    static @NonNull
    Queue<IInterceptor> load(@NonNull RouterMeta meta) {
        Set<Class<? extends IInterceptor>> allInterceptors = new ArraySet<>(globalInterceptor);
        Class<? extends IInterceptor>[] interceptors = meta.getInterceptors();
        if (interceptors != null) {
            allInterceptors.addAll(Arrays.asList(interceptors));
        }
        Queue<IInterceptor> result = new PriorityQueue<>(11, new InterceptorComparator());
        for (Class<? extends IInterceptor> interceptorClass : allInterceptors) {
            result.add(getInstance(interceptorClass));
        }
        return result;
    }

    // from large to small
    private static class InterceptorComparator implements Comparator<IInterceptor> {
        @Override
        public int compare(IInterceptor o1, IInterceptor o2) {
            int priority1 = RouterStore.getInterceptors().get(o1.getClass()).getPriority();
            int priority2 = RouterStore.getInterceptors().get(o2.getClass()).getPriority();
            return priority2 - priority1;
        }
    }

    @SuppressWarnings("unchecked")
    private static IInterceptor getInstance(Class<? extends IInterceptor> clz) {
        IInterceptor t = instanceMap.get(clz);
        if (t == null && weakInstanceMap.containsKey(clz)) {
            t = weakInstanceMap.get(clz).get();
        }
        if (t == null) {
            synchronized (InterceptorLoader.class) {
                t = instanceMap.get(clz);
                if (t == null && weakInstanceMap.containsKey(clz)) {
                    t = weakInstanceMap.get(clz).get();
                }
                if (t == null) {
                    RouterMeta meta = RouterStore.getInterceptors().get(clz);
                    if (meta == null) {
                        meta = RouterMeta.build(RouterType.INTERCEPTOR).assembleInterceptor(clz, 0, false, Cache.NO);
                        RouterStore.getInterceptors().put(clz, meta);
                    }
                    t = (IInterceptor) ReflectUtil.getInstance(clz);
                    if (meta.getCache() == Cache.SINGLETON) {
                        instanceMap.put(clz, t);
                    } else if (meta.getCache() == Cache.WEAK) {
                        weakInstanceMap.put(clz, new WeakReference<>(t));
                    }
                }
            }
        }
        return t;
    }

}
