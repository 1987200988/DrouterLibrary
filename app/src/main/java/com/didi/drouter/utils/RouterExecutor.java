/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.didi.drouter.annotation.Thread;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.RestrictTo;

/**
 * Created by gaowei on 2018/9/17
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RouterExecutor {

    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void execute(@Thread int mode, Runnable runnable) {
        switch (mode) {
            case Thread.POSTING:
                runnable.run();
                break;
            case Thread.MAIN:
                main(runnable);
                break;
            case Thread.WORKER:
                worker(runnable);
                break;
            default:
                runnable.run();
        }
    }

    public static void main(Runnable runnable) {
        main(runnable, 0);
    }

    public static void main(Runnable runnable, long timeDelay) {
        if (java.lang.Thread.currentThread() == Looper.getMainLooper().getThread() && timeDelay == 0) {
            runnable.run();
        } else {
            mainHandler.postDelayed(runnable, timeDelay);
        }
    }

    public static void worker(Runnable runnable) {
        if (java.lang.Thread.currentThread() == Looper.getMainLooper().getThread()) {
            threadPool.submit(runnable);
        } else {
            runnable.run();
        }
    }

    public static void submit(Runnable runnable) {
        threadPool.submit(runnable);
    }

}
