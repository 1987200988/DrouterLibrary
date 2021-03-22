/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.example.drouterdemo;


import com.didi.drouter.remote.IRemoteCallback;


public interface ITest {

    void test(int a , IRemoteCallback callback);
}