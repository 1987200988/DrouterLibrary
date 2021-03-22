/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.router;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.didi.drouter.api.Extend;
import com.didi.drouter.api.RouterType;
import com.didi.drouter.interceptor.InterceptorCallback;
import com.didi.drouter.interceptor.InterceptorHandler;
import com.didi.drouter.remote.RemoteBridge;
import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.store.RouterStore;
import com.didi.drouter.store.Statistics;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2018/6/22
 */
class RouterLoader {

    private Request request;
    private LifecycleOwner lifecycleOwner;
    private int startThread;
    private int callbackThread;
    private String authority;
    private boolean resend;
    private RouterCallback callback;

    private RouterLoader() {}

    @NonNull
    static RouterLoader build(Request request, LifecycleOwner lifecycleOwner,
                              int startThread, int callbackThread,
                              String authority, boolean resend,
                              RouterCallback callback) {
        RouterLoader loader = new RouterLoader();
        loader.request = request;
        loader.lifecycleOwner = lifecycleOwner;
        loader.startThread = startThread;
        loader.callbackThread = callbackThread;
        loader.authority = authority;
        loader.resend = resend;
        loader.callback = callback;
        return loader;
    }

    void start() {
        RouterExecutor.execute(startThread, new Runnable() {
            @Override
            public void run() {
                RouterLogger.getCoreLogger().d(
                        "---------------------------------------------------------------------------");
                RouterLogger.getCoreLogger().d("original request \"%s\", router uri \"%s\", thread %s, need callback \"%s\"",
                        request.getNumber(), request.getUri(), TextUtils.getThreadMode(startThread), callback != null);
                if (TextUtils.isEmpty(authority)) {
                    startLocal();
                } else {
                    startRemote();
                }
            }
        });
    }

    private void startLocal() {
        Statistics.track("local_request");
        TextUtils.appendExtra(request.getExtra(), TextUtils.getQuery(request.getUri()));
        Map<Request, RouterMeta> requestMap = makeRequest();

        if (requestMap.isEmpty()) {
            RouterLogger.getCoreLogger().w("warning: there is no request target match");
            Result result = new Result(request, null, lifecycleOwner, callbackThread, callback);
            result.putExtra(ResultAgent.FIELD_RESULT_STATE + request.getNumber(), ResultAgent.STATE_NOT_FOUND);
            result.complete(request);
            return;
        }

        final Result result = new Result(request, requestMap.keySet(), lifecycleOwner, callbackThread, callback);
        if (requestMap.size() > 1) {
            RouterLogger.getCoreLogger().w("warning: request match %s targets", requestMap.size());
        }
        for (final Map.Entry<Request, RouterMeta> entry : requestMap.entrySet()) {
            InterceptorHandler.handle(entry.getKey(), entry.getValue(), new InterceptorCallback() {
                @Override
                public void onContinue() {
                    RouterDispatcher.start(entry.getKey(), entry.getValue(), result, callback);
                }

                @Override
                public void onInterrupt() {
                    result.putExtra(ResultAgent.FIELD_RESULT_STATE + request.getNumber(), ResultAgent.STATE_INTERCEPT);
                    result.complete(entry.getKey());
                }
            });
        }
    }

    @NonNull
    private Map<Request, RouterMeta> makeRequest() {
        Map<Request, RouterMeta> requestMap = new ArrayMap<>();
        Parcelable parcelable = request.getParcelable(Extend.REQUEST_ACTIVITY_START_VIA_INTENT);
        if (parcelable instanceof Intent) {
            request.getExtra().remove(Extend.REQUEST_ACTIVITY_START_VIA_INTENT);
            Intent intent = (Intent) parcelable;
            RouterLogger.getCoreLogger().d("request %s, intent \"%s\"", request.getNumber(), intent);
            PackageManager pm = request.getContext().getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (activities != null && !activities.isEmpty()) {
                Request request;
                try {
                    request = this.request.createBranchInternal(false, RouterType.ACTIVITY, 0);
                    RouterLogger.getCoreLogger().d("request \"%s\" find target class \"%s\", type \"%s\"",
                            request.getNumber(), activities.get(0).activityInfo.name, "activity");
                    requestMap.put(request, RouterMeta.build(RouterType.ACTIVITY).assembleRouter(intent));
                } catch (CloneNotSupportedException e) {
                    RouterLogger.getCoreLogger().e("makeRequest error: %s", e);
                }
            }
        } else {
            Set<RouterMeta> metas = getAllRouterMetas();
            int index = 0;
            for (RouterMeta routerMeta : metas) {
                Request request;
                try {
                    request = this.request.createBranchInternal(metas.size() > 1, routerMeta.getRouterType(), index++);
                    RouterLogger.getCoreLogger().d("request \"%s\" find target class \"%s\", type \"%s\"",
                            request.getNumber(), routerMeta.getSimpleClassName(), routerMeta.getRouterType());
                    requestMap.put(request, routerMeta);
                } catch (CloneNotSupportedException e) {
                    RouterLogger.getCoreLogger().e("makeRequest error: %s", e);
                }
            }
        }
        return requestMap;
    }

    @NonNull
    private Set<RouterMeta> getAllRouterMetas() {
        Set<RouterMeta> matchMetas = RouterStore.getRouterMetas(TextUtils.getUriKey(request.getUri()));
        SparseArray<RouterMeta> sparseArray = new SparseArray<>();
        String schemeHost = request.getString(Extend.REQUEST_ACTIVITY_DEFAULT_SCHEME_HOST);
        if (!TextUtils.isEmpty(schemeHost) && request.getUri().toString().startsWith(schemeHost.toLowerCase())) {
            Set<RouterMeta> degradeMetas = RouterStore.getRouterMetas(TextUtils.getUriKey(request.getUri().getPath()));
            for (RouterMeta meta : degradeMetas) {
                if (meta.getRouterType() == RouterType.ACTIVITY) {
                    matchMetas.add(meta);
                }
            }
        }
        Set<RouterMeta> exactMetas = new ArraySet<>();
        for (RouterMeta meta : matchMetas) {
            if (meta.getRouterType() == RouterType.ACTIVITY) {
                if (sparseArray.get(0) != null) {
                    RouterLogger.getCoreLogger().w("warning: request match more than one activity and this \"%s\" will be ignored",
                            meta.getSimpleClassName());
                } else {
                    sparseArray.put(0, meta);
                }
            } else if (meta.getRouterType() == RouterType.FRAGMENT) {
                if (sparseArray.get(1) != null) {
                    RouterLogger.getCoreLogger().w("warning: request match more than one fragment and this \"%s\" will be ignored",
                            meta.getSimpleClassName());
                } else {
                    sparseArray.put(1, meta);
                }
            } else if (meta.getRouterType() == RouterType.VIEW) {
                if (sparseArray.get(2) != null) {
                    RouterLogger.getCoreLogger().w("warning: request match more than one view and this \"%s\" will be ignored",
                            meta.getSimpleClassName());
                } else {
                    sparseArray.put(2, meta);
                }
            } else {
                exactMetas.add(meta);
            }
        }
        if (sparseArray.get(0) != null) {
            exactMetas.add(sparseArray.get(0));
        } else if (sparseArray.get(1) != null) {
            exactMetas.add(sparseArray.get(1));
        } else if (sparseArray.get(2) != null) {
            exactMetas.add(sparseArray.get(2));
        }
        return exactMetas;
    }

    private void startRemote() {
        Statistics.track("remote_request");
        Result result = new Result(request, Collections.singleton(request), lifecycleOwner, callbackThread, callback);
        RemoteBridge.load(authority, resend, lifecycleOwner != null ? new WeakReference<>(lifecycleOwner) : null)
                .start(request, result, callback);
    }

}
