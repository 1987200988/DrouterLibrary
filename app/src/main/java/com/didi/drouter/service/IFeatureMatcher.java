/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.service;

/**
 * Created by gaowei on 2018/9/26
 */
public interface IFeatureMatcher<T> {

    boolean match(T object);
}
