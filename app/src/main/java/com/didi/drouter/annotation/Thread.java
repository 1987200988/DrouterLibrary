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
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.SOURCE)
@IntDef({Thread.POSTING, Thread.MAIN, Thread.WORKER})
public @interface Thread {

    /**
     * no thread operation
     */
    int POSTING = 0;

    /**
     * change to main thread
     */
    int MAIN = 1;

    /**
     * change to worker thread
     */
    int WORKER = 2;
}
