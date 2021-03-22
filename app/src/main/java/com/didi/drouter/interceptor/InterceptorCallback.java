/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.interceptor;

/**
 * Created by gaowei on 2018/8/31
 */
public interface InterceptorCallback {

    void onContinue();

    void onInterrupt();

}
