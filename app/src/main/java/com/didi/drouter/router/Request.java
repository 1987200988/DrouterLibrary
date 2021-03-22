/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.router;

import java.util.concurrent.atomic.AtomicInteger;

import com.didi.drouter.annotation.Thread;
import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.RouterType;
import com.didi.drouter.visible.ActivityCallback;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2018/8/31
 */
public class Request extends DataExtras<Request> implements Cloneable {

    private static AtomicInteger counter = new AtomicInteger(0);

    private Uri uri;
    private Context context;
    private RouterType type;
    private int startThread = Thread.POSTING;
    private int callbackThread = Thread.POSTING;
    private LifecycleOwner lifecycleOwner;
    private String authority;
    private boolean resend;
    private String serialNumber;

    private Request(@NonNull Uri uri) {
        this.uri = uri;
        this.serialNumber = String.valueOf(counter.getAndIncrement());
    }

    public static Request build(String uri) {
        return new Request(uri == null ? Uri.EMPTY : Uri.parse(uri));
    }

    public void start(Context context) {
        start(context, (RouterCallback) null);
    }

    public void start(Context context, ActivityCallback callback) {
        start(context, (RouterCallback) callback);
    }

    public void start(Context context, RouterCallback callback) {
        this.context = context == null ? DRouter.getContext() : context;
        RouterLoader.build(this, lifecycleOwner, startThread, callbackThread,
                authority, resend, callback).start();
    }

    public @NonNull Context getContext() {
        return context;
    }

    public @NonNull Uri getUri() {
        return uri;
    }

    public RouterType getType() {
        return type;
    }

    public String getNumber() {
        return serialNumber;
    }

    public Request setThread(@Thread int startThread, @Thread int callbackThread) {
        this.startThread = startThread;
        this.callbackThread = callbackThread;
        return this;
    }

    public Request setLifecycleOwner(LifecycleOwner owner) {
        this.lifecycleOwner = owner;
        return this;
    }

    public Request setRemoteAuthority(String authority) {
        this.authority = authority;
        return this;
    }

    public Request setRemoteResend(boolean resend) {
        this.resend = resend;
        return this;
    }

    Request createBranchInternal(boolean clone, RouterType type, int targetIndex)
            throws CloneNotSupportedException {
        Request request = this;
        if (clone) {
            request = (Request) super.clone();
            request.serialNumber = request.getNumber() + "_" + targetIndex;
        }
        request.type = type;
        return request;
    }
}
