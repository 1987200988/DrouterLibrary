/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.api;

import com.didi.drouter.router.IRouterHandler;
import com.didi.drouter.router.Request;
import com.didi.drouter.service.ServiceLoader;
import com.didi.drouter.store.IRegister;
import com.didi.drouter.store.RouterKey;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.store.ServiceKey;
import com.didi.drouter.utils.SystemUtil;

import android.app.Application;
import androidx.annotation.NonNull;

/**
 * Created by gaowei on 2018/8/31
 */
public class DRouter {

    public static Application getContext() {
        return SystemUtil.getApplication();
    }

    /**
     * This init method can be ignored, it will be executed automatically.
     * You can also execute it manually by your self or in worker thread.
     * @param application Application
     */
    public static void init(Application application) {
        SystemUtil.setApplication(application);
        RouterStore.load(RouterStore.HOST);
    }

    /**
     * Navigation to activity or handler
     * there will be only one activity match at most, but may be several router handler.
     * @param uri String
     * @return request
     */
    @NonNull
    public static Request build(String uri) {
        return Request.build(uri);
    }

    /**
     * Navigation to service annotation.
     * @param function service interface in service annotation
     * @return ServiceLoader
     */
    @NonNull
    public static <T> ServiceLoader<T> build(@NonNull Class<T> function) {
        return ServiceLoader.build(function);
    }

    /**
     * Register dynamic handler
     */
    @NonNull
    public static IRegister register(RouterKey key, IRouterHandler handler) {
        return RouterStore.register(key, handler);
    }

    /**
     * Register dynamic service
     */
    @NonNull
    public static <T> IRegister register(ServiceKey<T> key, T service) {
        return RouterStore.register(key, service);
    }

}
