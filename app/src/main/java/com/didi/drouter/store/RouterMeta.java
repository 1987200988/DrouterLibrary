/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.store;

import com.didi.drouter.api.RouterType;
import com.didi.drouter.interceptor.IInterceptor;
import com.didi.drouter.router.IRouterHandler;
import com.didi.drouter.service.IFeatureMatcher;
import com.didi.drouter.utils.TextUtils;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Created by gaowei on 2018/8/30
 */
@SuppressWarnings("all")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RouterMeta {

    private RouterType routerType;
    private Class<?> targetClass;    // fragment, view, static handler, service, interceptor
    private int priority;            // interceptor, service

    // for router
    private @NonNull
    String scheme;
    private @NonNull String host;
    private @NonNull String path;    // 如果path不是正则也不是""，就必须/开头
    private String activityName;
    private @Nullable
    Class<? extends IInterceptor>[] interceptors;   //impl
    private int thread;
    private boolean waiting;
    private Intent intent;
    private RouterKey routerKey;
    private IRouterHandler handler;

    // for service
    private String serviceAlias;
    private @Nullable IFeatureMatcher<Object> featureMatcher;   //instance
    private ServiceKey serviceKey;
    private boolean unregisterAfterExecute;
    private Object service;

    // for interceptor
    private boolean global;

    // for service and interceptor
    private int cache;

    private RouterMeta(RouterType routerType) {
        this.routerType = routerType;
    }

    public static RouterMeta build(RouterType routerType) {
        return new RouterMeta(routerType);
    }

    // key is uri, for activity
    public RouterMeta assembleRouter(String scheme, String host, String path, String targetClassName,
                                     Class<? extends IInterceptor>[] interceptors, int thread, boolean waiting) {
        this.scheme = scheme;
        this.host = host;
        this.path = path;
        this.activityName = targetClassName;
        this.interceptors = interceptors;
        this.thread = thread;
        this.waiting = waiting;
        return this;
    }

    // key is uri, for others
    public RouterMeta assembleRouter(String scheme, String host, String path, Class<?> targetClass,
                                     Class<? extends IInterceptor>[] interceptors, int thread, boolean waiting) {
        this.scheme = scheme;
        this.host = host;
        this.path = path;
        this.targetClass = targetClass;
        this.interceptors = interceptors;
        this.thread = thread;
        this.waiting = waiting;
        return this;
    }

    public RouterMeta assembleRouter(Intent intent) {
        this.intent = intent;
        return this;
    }

    // for dynamic handler
    public void setHandler(RouterKey key, @NonNull IRouterHandler handler) {
        this.routerKey = key;
        this.handler = handler;
    }

    // key is function
    public RouterMeta assembleService(Class<?> serviceClass, String alias,
                                      IFeatureMatcher<Object> featureMatcher, int priority, int cache) {
        this.targetClass = serviceClass;
        this.serviceAlias = alias;
        this.featureMatcher = featureMatcher;
        this.priority = priority;
        this.cache = cache;
        return this;
    }

    // for dynamic service
    public void setService(ServiceKey key, Object service, boolean unregisterAfterExecute) {
        this.serviceKey = key;
        this.service = service;
        this.unregisterAfterExecute = unregisterAfterExecute;
    }

    // key is string name or impl
    public RouterMeta assembleInterceptor(Class<? extends IInterceptor> interceptorClass, int priority,
                                          boolean global, int cache) {
        this.targetClass = interceptorClass;
        this.priority = priority;
        this.global = global;
        this.cache = cache;
        return this;
    }

    public RouterType getRouterType() {
        return routerType;
    }

    public String getActivityClassName() {
        return activityName;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public String getSimpleClassName() {
        if (activityName != null) {
            return activityName.substring(activityName.lastIndexOf(".") + 1);
        } else if (targetClass != null) {
            return targetClass.getSimpleName();
        } else if (handler != null) {
            return handler.getClass().getName().substring(handler.getClass().getName().lastIndexOf(".") + 1);
        } else {
            return null;
        }
    }

    // for router
    public Class<? extends IInterceptor>[] getInterceptors() {
        return interceptors;
    }

    public int getThread() {
        return thread;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public Intent getIntent() {
        return intent;
    }

    // scheme host path 任何一个是正则表达式
    public boolean isRegexMatch(Uri uri) {
        String s = uri.getScheme();
        String h = uri.getHost();
        String p = uri.getPath();
        return s != null && s.matches(scheme) && h != null && h.matches(host) && p != null && p.matches(path);
    }

    // 标识符和/以外的字符
    public boolean isRegexUri() {
        return TextUtils.isRegex(scheme) || TextUtils.isRegex(host) || TextUtils.isRegex(path);
    }

    public String getLegalUri() {
        return scheme + "://" + host + path;
    }

    public IRouterHandler getHandler() {
        return handler;
    }

    // for service
    public String getServiceAlias() {
        return serviceAlias;
    }

    public int getCache() {
        return cache;
    }

    public ServiceKey getServiceKey() {
        return serviceKey;
    }

    public boolean isUnregisterAfterExecute() {
        return unregisterAfterExecute;
    }

    public Object getService() {
        return service;
    }

    @Nullable
    public IFeatureMatcher<Object> getFeatureMatcher() {
        return featureMatcher;
    }

    // for interceptor
    public boolean isGlobal() {
        return global;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
