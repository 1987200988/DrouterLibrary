/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.router;

import com.didi.drouter.api.Extend;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;

import android.os.Handler;
import android.os.HandlerThread;

/**
 * Created by gaowei on 2019/1/17
 */
class Monitor {

    private static Handler timeoutHandler;

    static void startMonitor(final Request request, final Result result) {
        int period = request.getExtra().getInt(Extend.REQUEST_WAITING_TIMEOUT_PERIOD);
        if (period > 0) {
            check();
            RouterLogger.getCoreLogger().d("monitor for request \"%s\" start, count down \"%sms\"", request.getNumber(), period);
            timeoutHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    RouterExecutor.submit(new Runnable() {
                        @Override
                        public void run() {
                            result.putExtra(ResultAgent.FIELD_RESULT_STATE + request.getNumber(), ResultAgent.STATE_TIMEOUT);
                            result.complete(request);
                        }
                    });
                }
            }, period);
        }
    }

    private static void check() {
        if (timeoutHandler == null) {
            synchronized (Monitor.class) {
                if (timeoutHandler == null) {
                    HandlerThread handlerThread = new HandlerThread("timeout-monitor-thread");
                    handlerThread.start();
                    timeoutHandler = new Handler(handlerThread.getLooper());
                }
            }
        }
    }
}
