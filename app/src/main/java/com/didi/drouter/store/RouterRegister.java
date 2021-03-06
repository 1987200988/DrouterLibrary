/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.store;

import com.didi.drouter.router.IRouterHandler;

/**
 * Created by gaowei on 2019-04-29
 */
public class RouterRegister implements IRegister {

    private RouterKey uriKey;
    private IRouterHandler handler;

    private ServiceKey serviceKey;
    private Object service;

    private boolean isSuccess;

    public RouterRegister(RouterKey uriKey, IRouterHandler handler, boolean isSuccess) {
        this.uriKey = uriKey;
        this.handler = handler;
        this.isSuccess = isSuccess;
    }

    public RouterRegister(ServiceKey key, Object service, boolean isSuccess) {
        this.serviceKey = key;
        this.service = service;
        this.isSuccess = isSuccess;
    }

    @Override
    public void unregister() {
        if (isSuccess) {
            RouterStore.unregister(uriKey, handler);
            RouterStore.unregister(serviceKey, service);
        }
    }

    @Override
    public boolean isSuccess() {
        return isSuccess;
    }
}
