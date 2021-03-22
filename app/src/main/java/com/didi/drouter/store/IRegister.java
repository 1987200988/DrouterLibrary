/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.store;

/**
 * Created by gaowei on 2019-04-29
 */
public interface IRegister {

    void unregister();

    boolean isSuccess();
}
