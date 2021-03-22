/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.IntDef;

/**
 * Created by gaowei on 2019/2/17
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@IntDef({Cache.NO, Cache.WEAK, Cache.SINGLETON})
public @interface Cache {

    int NO = 0;

    int WEAK = 1;

    int SINGLETON = 2;
}
