/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.interceptor;

import java.util.Queue;

import com.didi.drouter.router.Request;
import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.utils.RouterLogger;

import androidx.annotation.NonNull;

/**
 * Created by gaowei on 2018/9/6
 */
public class InterceptorHandler {

    public static void handle(final Request request, RouterMeta meta, final InterceptorCallback callback) {
        RouterLogger.getCoreLogger().d(">> Enter interceptors, request \"%s\"", request.getNumber());
        Queue<IInterceptor> interceptors = InterceptorLoader.load(meta);
        handleNext(interceptors, request, callback);
    }

    private static void handleNext(@NonNull final Queue<IInterceptor> interceptors, final Request request, final InterceptorCallback callback) {
        final IInterceptor interceptor = interceptors.poll();
        if (interceptor == null) {
            RouterLogger.getCoreLogger().d("<< Pass all interceptors, request \"%s\"", request.getNumber());
            callback.onContinue();
            return;
        }

        RouterMeta interceptorMeta = RouterStore.getInterceptors().get(interceptor.getClass());
        RouterLogger.getCoreLogger().d("interceptor \"%s\" execute, for request \"%s\", global:%s, priority:%s",
                interceptor.getClass().getSimpleName(), request.getNumber(), interceptorMeta.isGlobal(), interceptorMeta.getPriority());
        interceptor.handle(request, new InterceptorCallback() {

            @Override
            public void onContinue() {
                handleNext(interceptors, request, callback);
            }

            @Override
            public void onInterrupt() {
                RouterLogger.getCoreLogger().w("request \"%s\" interrupt by \"%s\"", request.getNumber(), interceptor.getClass().getSimpleName());
                callback.onInterrupt();
            }
        });
    }


}
