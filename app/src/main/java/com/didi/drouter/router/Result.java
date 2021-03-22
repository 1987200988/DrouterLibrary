/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.router;

import java.util.Set;

import com.didi.drouter.annotation.Thread;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2019/1/9
 */
public class Result extends DataExtras<Result> {

    private int routerSize;
    ResultAgent agent;

    boolean isActivityStarted;
    boolean isVisibleTransacted;
    Class visibleClass;   // for fragment/view only
    Object visibleInstance;

    public Result(@NonNull Request request, @Nullable Set<Request> targetRequests,
                  LifecycleOwner lifecycleOwner, @Thread int callbackThread, RouterCallback callback) {
        agent = new ResultAgent(this, request, targetRequests, lifecycleOwner, callbackThread, callback);
        routerSize = targetRequests != null ? targetRequests.size() : 0;
    }

    // 本次请求共有多少个匹配目标
    public int getRouterSize() {
        return routerSize;
    }

    public boolean isActivityStarted() {
        return isActivityStarted;
    }

    public boolean isVisibleTransacted() {
        return isVisibleTransacted;
    }

    public Class getVisibleClass() {
        return visibleClass;
    }

    public Object getVisibleInstance() {
        return visibleInstance;
    }

    // 用于从Activity中获取RequestNumber
    public static String getRequestNumber(@Nullable Intent intent) {
        return ResultAgent.getRequestNumber(intent);
    }

    public static Request getRequest(String requestNumber) {
        return ResultAgent.getRequest(requestNumber);
    }

    public static Result getResult(String requestNumber) {
        return ResultAgent.getResult(requestNumber);
    }

    // 当waiting=true时有效，完成任务
    public static void complete(String requestNumber) {
        ResultAgent.complete(requestNumber);
    }

    // 当waiting=true时有效，完成任务
    public void complete(Request request) {
        ResultAgent.complete(request);
    }


}
