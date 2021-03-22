/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.service;


import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2018/9/4
 */
@SuppressWarnings("unchecked")
public class ServiceLoader<T> {

    private ServiceAgent<T> serviceAgent;

    private ServiceLoader(Class<T> function) {
        serviceAgent = new ServiceAgent<>(function);
    }

    public static @NonNull
    <T> ServiceLoader<T> build(Class function) {
        return new ServiceLoader<>(function);
    }

    public ServiceLoader<T> setAlias(String alias) {
        serviceAgent.setAlias(alias);
        return this;
    }

    public ServiceLoader<T> setFeature(Object feature) {
        serviceAgent.setFeature(feature);
        return this;
    }

    public ServiceLoader<T> setRemoteAuthority(String authority) {
        serviceAgent.setRemoteAuthority(authority);
        return this;
    }

    /**
     * If set, it will auto stop resend behavior when lifecycle is destroy.
     * It will take effect for all execute by this build,
     * for example, if this owner is destroyed, all the execute command resend will be stopped.
     * {@link ServiceLoader#setRemoteResend}
     */
    public ServiceLoader<T> setLifecycleOwner(LifecycleOwner owner) {
        serviceAgent.setLifecycleOwner(owner);
        return this;
    }

    /**
     * If set, and using feature meanwhile, please implement hashCode for feature class.
     * It will take effect for every execute.
     * {@link ServiceLoader#setFeature}
     */
    public ServiceLoader<T> setRemoteResend(boolean resend) {
        serviceAgent.setRemoteResend(resend);
        return this;
    }

    public T getService(Object... parameter) {
        return serviceAgent.getService(parameter);
    }

    public @NonNull List<T> getAllService(Object... parameter) {
        return serviceAgent.getAllService(parameter);
    }

    public Class<? extends T> getServiceClass() {
        return serviceAgent.getServiceClass();
    }

    public @NonNull List<Class<? extends T>> getAllServiceClass() {
        return serviceAgent.getAllServiceClass();
    }
}