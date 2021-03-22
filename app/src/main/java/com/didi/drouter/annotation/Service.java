/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.Keep;

/**
 * Created by gaowei on 2018/8/30
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Keep
public @interface Service {

    /**
     * use parent interface or class
     * also you can use AnyAbility.class which represent all its parent interfaces and classes
     */
    Class<?>[] function();

    /**
     * alias array will be matched with function array one by one
     */
    String[] alias() default {};

    /**
     * feature array will be matched with function array one by one
     */
    Class<?>[] feature() default {};

    /**
     * from large to small
     */
    int priority() default 0;

    @Cache
    int cache() default Cache.NO;
}
