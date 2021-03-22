/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.router;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.api.Extend;
import com.didi.drouter.store.RouterMeta;
import com.didi.drouter.utils.ReflectUtil;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.TextUtils;
import com.didi.drouter.visible.ActivityCallback;
import com.didi.drouter.visible.HoldFragmentForActivity;
import com.didi.drouter.visible.IVisibleTransition;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

/**
 * Created by gaowei on 2018/9/5
 */
class RouterDispatcher {

    static void start(Request request, RouterMeta meta, Result result, RouterCallback callback) {

        RouterLogger.getCoreLogger().d("request \"%s\" execute, thread: %s", request.getNumber(), TextUtils.getThreadMode(meta.getThread()));
        switch (meta.getRouterType()) {
            case ACTIVITY:
                startActivity(request, meta, result, callback);
                break;
            case FRAGMENT:
                startFragment(request, meta, result);
                break;
            case VIEW:
                startView(request, meta, result);
                break;
            case HANDLER:
                startHandler(request, meta, result);
                break;
        }
    }

    private static void startActivity(Request request, RouterMeta meta, Result result, RouterCallback callback) {
        Context context = request.getContext();
        Intent intent = meta.getIntent();
        if (intent == null) {
            intent = new Intent();
            intent.setClassName(context, meta.getActivityClassName());
        }
        if (request.getExtra().containsKey(Extend.FIELD_START_ACTIVITY_FLAGS)) {
            intent.setFlags(request.getInt(Extend.FIELD_START_ACTIVITY_FLAGS));
        }
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra(ResultAgent.FIELD_REQUEST_NUMBER, request.getNumber());
        intent.putExtras(request.getExtra());
        if (context instanceof Activity && callback instanceof ActivityCallback) {
            HoldFragmentForActivity.start((Activity) context, intent, 1024, (ActivityCallback) callback);
        } else if (context instanceof Activity && request.getExtra().containsKey(Extend.FIELD_START_ACTIVITY_REQUEST_CODE)) {
            int requestCode = request.getInt(Extend.FIELD_START_ACTIVITY_REQUEST_CODE);
            ActivityCompat.startActivityForResult((Activity) context, intent, requestCode, intent.getBundleExtra(Extend.FIELD_START_ACTIVITY_OPTIONS));
        } else {
            ActivityCompat.startActivity(context, intent, intent.getBundleExtra(Extend.FIELD_START_ACTIVITY_OPTIONS));
        }
        int[] anim = request.getIntArray(Extend.FIELD_START_ACTIVITY_ANIMATION);
        if (context instanceof Activity && anim != null && anim.length == 2) {
            ((Activity) context).overridePendingTransition(anim[0], anim[1]);
        }
        result.isActivityStarted = true;
        if (!meta.isWaiting()) {
            result.complete(request);
        } else {
            RouterLogger.getCoreLogger().w("request \"%s\" will waiting", request.getNumber());
            Monitor.startMonitor(request, result);
        }
    }

    private static void startFragment(Request request, RouterMeta meta, Result result) {
        IVisibleTransition iVisibleTransition = DRouter.build(IVisibleTransition.class).getService();
        Class fragmentCls = meta.getTargetClass();
        result.visibleClass = fragmentCls;
        Object object = null;
        if (request.getExtra().getBoolean(Extend.REQUEST_FRAGMENT_NEW_INSTANCE, true)) {
            object = ReflectUtil.getInstance(fragmentCls);
            result.visibleInstance = object;
            if (object instanceof Fragment) {
                ((Fragment) object).setArguments(request.getExtra());
            } else if (object instanceof android.app.Fragment) {
                ((android.app.Fragment) object).setArguments(request.getExtra());
            }
        }
        if (iVisibleTransition != null) {
            result.isVisibleTransacted = iVisibleTransition.transact(request, fragmentCls, object);
        }
        result.complete(request);
    }

    private static void startView(Request request, RouterMeta meta, Result result) {
        IVisibleTransition iViewTransition = DRouter.build(IVisibleTransition.class).getService();
        Class viewCls = meta.getTargetClass();
        result.visibleClass = viewCls;
        Object object = null;
        if (request.getExtra().getBoolean(Extend.REQUEST_VIEW_NEW_INSTANCE, true)) {
            object = ReflectUtil.getInstance(viewCls, request.getContext());
            result.visibleInstance = object;
            if (object instanceof View) {
                ((View) object).setTag(request.getExtra());
            }
        }
        if (iViewTransition != null) {
            result.isVisibleTransacted = iViewTransition.transact(request, viewCls, object);
        }
        result.complete(request);
    }

    private static void startHandler(final Request request, final RouterMeta meta, final Result result) {
        Object instance = meta.getHandler();
        if (instance == null) {
            Class handlerCls = meta.getTargetClass();
            instance = ReflectUtil.getInstance(handlerCls);
        }
        final Object handler = instance;
        RouterExecutor.execute(meta.getThread(), new Runnable() {
            @Override
            public void run() {
                if (handler instanceof IRouterHandler) {
                    if (meta.isWaiting()) {
                        RouterLogger.getCoreLogger().w("request \"%s\" will waiting", request.getNumber());
                    }
                    ((IRouterHandler) handler).handle(request, result);
                    if (!meta.isWaiting()) {
                        result.complete(request);
                    } else {
                        Monitor.startMonitor(request, result);
                    }
                } else {
                    result.putExtra(ResultAgent.FIELD_RESULT_STATE + request.getNumber(), ResultAgent.STATE_ERROR);
                    result.complete(request);
                }
            }
        });
    }

}
