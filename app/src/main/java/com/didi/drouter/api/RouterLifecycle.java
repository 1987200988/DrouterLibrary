/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.api;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

/**
 * Created by gaowei on 2020/9/26
 */
public class RouterLifecycle implements LifecycleOwner {
    
    private LifecycleRegistry lifecycle = new LifecycleRegistry(this);

    public RouterLifecycle() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }
    
    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void create() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
    }
    
    public void destroy() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
    }
}
