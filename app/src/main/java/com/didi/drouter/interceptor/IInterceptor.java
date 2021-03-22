/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.interceptor;

import com.didi.drouter.router.Request;

import androidx.annotation.NonNull;

/**
 * Created by gaowei on 2018/8/31
 */
public interface IInterceptor {

    void handle(@NonNull Request request, @NonNull InterceptorCallback callback);
}
