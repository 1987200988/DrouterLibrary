/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.router;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.didi.drouter.annotation.Thread;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2019/1/9
 */
class ResultAgent {

    static final String FIELD_RESULT_STATE = "field_result_state";
    static final String FIELD_REQUEST_NUMBER = "field_request_number";

    static final String STATE_NOT_FOUND = "not_found";
    static final String STATE_TIMEOUT = "timeout";
    static final String STATE_ERROR = "error";
    static final String STATE_INTERCEPT = "intercept";
    static final String STATE_COMPLETE = "complete";
    static final String STATE_REQUEST_CANCEL = "request_cancel";

    // key is request number, to remove one by one, include origin and all targets
    private static final Map<String, Result> serialToResult = new ConcurrentHashMap<>();
    // key is request number, not include origin
    private Map<String, Request> targetRequestMap = new ArrayMap<>();
    private Map<String, String> targetStateMap = new ArrayMap<>();    // add one by one
    // original
    private @NonNull
    Request originalRequest;
    private int callbackThread;
    private RouterCallback callback;

    ResultAgent(@NonNull final Result result, @NonNull final Request originalRequest, @Nullable Set<Request> targetRequests,
                LifecycleOwner lifecycleOwner, @Thread int callbackThread, RouterCallback callback) {
        serialToResult.put(originalRequest.getNumber(), result);
        this.originalRequest = originalRequest;
        this.callbackThread = callbackThread;
        this.callback = callback;
        if (targetRequests != null) {
            for (Request target : targetRequests) {
                serialToResult.put(target.getNumber(), result);
                targetRequestMap.put(target.getNumber(), target);
            }
        }
        if (lifecycleOwner != null && lifecycleOwner.getLifecycle() != null) {
            lifecycleOwner.getLifecycle().addObserver(new GenericLifecycleObserver() {
                @Override
                public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_DESTROY && serialToResult.containsKey(originalRequest.getNumber())) {
                        RouterLogger.getCoreLogger().w("request \"%s\" lifecycleOwner \"%s\" destroy and complete",
                                originalRequest.getNumber(), source.getClass().getSimpleName());
                        ResultAgent.this.callback = null;
                        result.putExtra(FIELD_RESULT_STATE + originalRequest.getNumber(), STATE_REQUEST_CANCEL);
                        complete(originalRequest.getNumber());
                    }
                }
            });
        }
    }

    static String getRequestNumber(@Nullable Intent activityIntent) {
        String number = activityIntent != null ? activityIntent.getStringExtra(FIELD_REQUEST_NUMBER) : null;
        return number == null ? "" : number;
    }

    static Request getRequest(String requestNumber) {
        Result result = serialToResult.get(requestNumber);
        if (result != null) {
            return result.agent.targetRequestMap.get(requestNumber);
        }
        return null;
    }

    static Result getResult(String requestNumber) {
        return serialToResult.get(requestNumber);
    }

    static void callback(String requestNumber) {
        Result result = serialToResult.get(requestNumber);
        if (result != null) {
            RouterLogger.getCoreLogger().d("request \"%s\" notify", requestNumber);
            callback(result, requestNumber);
        }
    }

    static void complete(Request request) {
        if (request != null) {
            complete(request.getNumber());
        }
    }

    // requestNumber可以是原始也可以是分支
    synchronized static void complete(String requestNumber) {
        Result result = serialToResult.get(requestNumber);
        if (result != null) {
            String state = STATE_COMPLETE;
            String key = FIELD_RESULT_STATE + requestNumber;
            if (result.getExtra().containsKey(key)) {
                state = result.getString(key);
                result.getExtra().remove(key);
            }
            if (result.agent.originalRequest.getNumber().equals(requestNumber)) {
                // all clear
                if (result.agent.targetRequestMap.size() > 1) {
                    RouterLogger.getCoreLogger().w("be careful, original request \"%s\" will be cleared", requestNumber);
                }
                if (result.agent.targetRequestMap.isEmpty()) {
                    completeOriginal(result); // no target
                } else {
                    for (String number : result.agent.targetRequestMap.keySet()) {
                        if (!result.agent.targetStateMap.containsKey(number)) {
                            completeOne(number, state);
                        }
                    }
                }
            } else {
                completeOne(requestNumber, state);
            }
        }
    }

    // requestNumber只可以是分支
    private synchronized static void completeOne(String requestNumber, String state) {
        Result result = serialToResult.get(requestNumber);
        if (result != null) {
            if (STATE_TIMEOUT.equals(state)) {
                RouterLogger.getCoreLogger().w("request \"%s\" time out and force-complete", requestNumber);
            }
            result.agent.targetStateMap.put(requestNumber, state);
            serialToResult.remove(requestNumber);
            RouterLogger.getCoreLogger().d("==== request \"%s\" complete, state \"%s\" ====", requestNumber, state);
            if (result.agent.targetStateMap.size() == result.agent.targetRequestMap.size()) {
                completeOriginal(result);
            }
        }
    }

    private synchronized static void completeOriginal(Result result) {
        RouterLogger.getCoreLogger().d("original request \"%s\" complete, state %s",
                result.agent.originalRequest.getNumber(), result.agent.targetStateMap.toString());
        serialToResult.remove(result.agent.originalRequest.getNumber());
        callback(result, result.agent.originalRequest.getNumber());
        if (!serialToResult.isEmpty()) {
            RouterLogger.getCoreLogger().w("serialToResult request remain be left: %s", Arrays.toString(serialToResult.keySet().toArray()));
        }
    }

    // may complete or not
    private static void callback(final Result result, final String requestNumber) {
        final boolean isOriginal = android.text.TextUtils.equals(result.agent.originalRequest.getNumber(), requestNumber);
        if (result.agent.callback != null) {
            RouterExecutor.execute(result.agent.callbackThread, new Runnable() {
                @Override
                public void run() {
                    RouterLogger.getCoreLogger().d((isOriginal ? "original " : "") + "request \"%s\" callback, thread %s",
                            requestNumber, TextUtils.getThreadMode(result.agent.callbackThread));
                    result.agent.callback.onResult(result);
                    end(result);
                }
            });
        } else {
            end(result);
        }
    }

    private static void end(Result result) {
        if (serialToResult.get(result.agent.originalRequest.getNumber()) == null) {
            RouterLogger.getCoreLogger().d(
                    "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            routerListener(result.agent.originalRequest, result);
        }
    }

    private static void routerListener(Request request, Result result) {
        IGlobalListener listener = DRouter.build(IGlobalListener.class).getService();
        if (listener != null) {
            listener.onComplete(request, result);
        }
    }
}
