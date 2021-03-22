/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.utils;

import com.didi.drouter.api.DRouter;

import android.util.Log;
import android.widget.Toast;

/**
 * Created by gaowei on 2018/9/6
 */
public class RouterLogger {

    public static final String NAME = "DRouterCore";
    private static IRouterLogger logger = new InnerLogger();
    private static RouterLogger coreLogger = new RouterLogger(NAME);
    private static RouterLogger appLogger = new RouterLogger("DRouterApp");

    private String TAG;

    private RouterLogger(String TAG) {
        this.TAG = TAG;
    }

    public static void setLogger(IRouterLogger logger) {
        RouterLogger.logger = logger;
    }

    public static RouterLogger getAppLogger() {
        return appLogger;
    }

    public static RouterLogger getCoreLogger() {
        return coreLogger;
    }

    public void d(String content, Object... args) {
        if (content != null && logger != null) {
            logger.d(TAG, format(content, args));
        }
    }

    public void w(String content, Object... args) {
        if (content != null && logger != null) {
            logger.w(TAG, format(content, args));
        }
    }

    public void e(String content, Object... args) {
        if (content != null && logger != null) {
            logger.e(TAG, format(content, args));
        }
    }

    public static void toast(final String string, final Object... args) {
        RouterExecutor.main(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DRouter.getContext(), format(string, args), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String format(String s, Object... args) {
        if (args == null) return s;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Throwable) {
                args[i] = Log.getStackTraceString((Throwable) args[i]);
            }
        }
        return String.format(s, args);
    }

    private static class InnerLogger implements IRouterLogger {

        @Override
        public void d(String TAG, String content) {
            if (SystemUtil.isDebug()) {
                Log.d(TAG, content);
            }
        }

        @Override
        public void w(String TAG, String content) {
            if (SystemUtil.isDebug()) {
                Log.w(TAG, content);
            }
        }

        @Override
        public void e(String TAG, String content) {
            if (SystemUtil.isDebug()) {
                Log.e(TAG, content);
            }
        }
    }
}
