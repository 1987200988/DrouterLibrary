/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.remote;

import java.util.concurrent.atomic.AtomicInteger;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.router.Request;
import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterCallback;
import com.didi.drouter.utils.ReflectUtil;
import com.didi.drouter.utils.RouterExecutor;
import com.didi.drouter.utils.RouterLogger;

import android.os.RemoteException;
import androidx.annotation.NonNull;

/**
 * Created by gaowei on 2018/11/2
 */
class RemoteDispatcher {

    private static final AtomicInteger count = new AtomicInteger(0);
    private RemoteResult remoteResult = new RemoteResult(RemoteResult.EXECUTING);

    @NonNull
    RemoteResult execute(final RemoteCommand command) {
        count.incrementAndGet();
        RouterLogger.getCoreLogger().d("[Service] command \"%s\" start, thread count %s", command, count.get());
        if (count.get() >= 16) {
            RouterLogger.getCoreLogger().e("[Service] binder thread pool is exploding", command, count.get());
        }
        if (command.uri != null) {
            if (count.get() >= 16) {
                RouterExecutor.submit(new Runnable() {
                    @Override
                    public void run() {
                        startRequest(command);
                    }
                });
            } else {
                startRequest(command);
            }
        } else if (command.serviceClass != null) {
            startService(command);
        }
        count.decrementAndGet();
        return remoteResult;
    }

    private void startRequest(final RemoteCommand command) {
        Request request = DRouter.build(command.uri);
        if (command.extra != null) {
            request.setExtra(command.extra);
        }
        if (command.addition != null) {
            request.setAddition(command.addition);
        }
        request.start(DRouter.getContext(), new RouterCallback() {
            @Override
            public void onResult(@NonNull Result result) {
                if (command.binder != null) {
                    RouterLogger.getCoreLogger().d("[Service] command \"%s\" result start callback", command);
                    RemoteCommand resultCommand = new RemoteCommand(RemoteCommand.REQUEST_RESULT);
                    resultCommand.isActivityStarted = result.isActivityStarted();
                    resultCommand.targetSize = result.getRouterSize();
                    resultCommand.extra = result.getExtra();
                    resultCommand.addition = result.getAddition();
                    try {
                        IClientService.Stub.asInterface(command.binder).callback(resultCommand);
                    } catch (RemoteException e) {
                        RouterLogger.getCoreLogger().e("[Service] command \"%s\" callback Exception %s", command, e);
                    }
                }
            }
        });
        remoteResult.state = RemoteResult.SUCCESS;
    }

    private void startService(final RemoteCommand command) {
        Object instance = DRouter.build(command.serviceClass).setAlias(command.alias).setFeature(command.feature).getService(command.constructor);
        RouterLogger.getCoreLogger().d("[Service] use drouter to build new service \"%s\", " +
                "and start invoke method \"%s\"",
                instance != null ? instance.getClass().getSimpleName() : null, command.methodName);
        try {
            if (instance != null) {
                remoteResult.result = ReflectUtil.invokeMethod(instance, command.methodName, command.parameters);
                remoteResult.state = RemoteResult.SUCCESS;
                return;
            }
        } catch (Exception e) {
            RouterLogger.getCoreLogger().e("[Service] invoke Exception %s", e);
        }
        remoteResult.state = RemoteResult.FAIL;
    }
}
