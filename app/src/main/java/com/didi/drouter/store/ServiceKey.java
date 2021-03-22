/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.store;



import com.didi.drouter.service.IFeatureMatcher;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2019/1/31
 */
public class ServiceKey<T> {

    Class<T> function;
    @NonNull
    String alias = "";
    IFeatureMatcher feature;
    boolean clearPrevious;
    boolean unregisterAfterExecute;
    LifecycleOwner lifecycleOwner;

    private ServiceKey() {}

    public static <T> ServiceKey<T> build(Class<T> function) {
        return build(function, false, false);
    }

    @SuppressWarnings("unchecked")
    public static <T> ServiceKey<T> build(Class<T> function, boolean clearPrevious, boolean unregisterAfterExecute) {
        ServiceKey key = new ServiceKey();
        key.function = function;
        key.clearPrevious = clearPrevious;
        key.unregisterAfterExecute = unregisterAfterExecute;
        return key;
    }

    public ServiceKey<T> setAlias(String alias) {
        this.alias = alias != null ? alias : "";
        return this;
    }

    public ServiceKey<T> setFeature(IFeatureMatcher feature) {
        this.feature = feature;
        return this;
    }

    public ServiceKey<T> setLifecycleOwner(LifecycleOwner owner) {
        this.lifecycleOwner = owner;
        return this;
    }
}
