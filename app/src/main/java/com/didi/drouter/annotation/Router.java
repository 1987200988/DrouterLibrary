/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.didi.drouter.interceptor.IInterceptor;

import androidx.annotation.Keep;

/**
 * Created by gaowei on 2019/1/30
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Keep
/**
 * used for Activity, Fragment, View, IRouterHandler
 */
public @interface Router {

    /**
     * scheme host path 支持正则表达式
     * 字段设置为""或者保持默认，表示只能匹配""，如果某个字段想匹配所有字符串，必须使用".*"表示
     */
    String scheme() default "";

    String host() default "";

    String path();    //如果不是正则表达式，请以/开头

    Class<? extends IInterceptor>[] interceptor() default {};

    @Thread
    int thread() default Thread.POSTING;

    /**
     * 用于 Activity 或 IRouterHandler 异步返回结果
     * 设置成true后必须在任意某个地方主动执行一次对应的 {@link com.didi.drouter.router.Result#complete(String)}
     * 否则无法完成任务，也不会回调RouterCallback返回结束
     */
    boolean waiting() default false;
}
