/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.remote;

import static com.didi.drouter.remote.RemoteProvider.BROADCAST_ACTION;
import static com.didi.drouter.remote.RemoteProvider.FIELD_REMOTE_LAUNCH_ACTION;
import static com.didi.drouter.remote.RemoteProvider.FIELD_REMOTE_PROCESS;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.didi.drouter.api.DRouter;
import com.didi.drouter.router.Request;
import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterCallback;
import com.didi.drouter.service.ServiceLoader;
import com.didi.drouter.utils.RouterLogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.lifecycle.GenericLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2018/11/1
 *
 * One service or request build will create one RemoteBridge instance,
 * but with multiple RemoteCommand when executes.
 *
 * Every resend RemoteCommand(execute) bind broadcast and one lifecycle observer if exists.
 */
public class RemoteBridge {

    private String authority;
    private boolean resend;
    private boolean reTry;
    private WeakReference<LifecycleOwner> lifecycle;

    // key是process, used for broadcast to resend
    private static Map<String, Set<RemoteCommand>> sResendCommandMap = new ConcurrentHashMap<>();
    // key是authority
    private static Map<String, IHostService> sHostServiceMap = new ConcurrentHashMap<>();
    // key是authority, value是process, one process register broadcast once
    private static Map<String, String> sProcessMap = new ConcurrentHashMap<>();

    private RemoteBridge() {}

    public static @NonNull
    RemoteBridge load(String authority, boolean resend, WeakReference<LifecycleOwner> lifecycle) {
        RemoteBridge remoteBridge = new RemoteBridge();
        remoteBridge.authority = authority;
        remoteBridge.resend = resend;
        remoteBridge.lifecycle = lifecycle;
        return remoteBridge;
    }

    public void start(final Request request, final Result result, RouterCallback callback) {
        RouterLogger.getCoreLogger().d("[Client] request \"%s\" start IPC", request.getNumber());

        final RemoteCommand command = new RemoteCommand(RemoteCommand.REQUEST);
        command.bridge = RemoteBridge.this;
        command.resend = resend;
        command.lifecycle = lifecycle;
        command.uri = request.getUri().toString();
        command.extra = request.getExtra();
        command.addition = request.getAddition();
        if (callback != null) {
            command.binder = new IClientService.Stub() {
                @Override
                public RemoteResult callback(RemoteCommand resultCommand) throws RemoteException {
                    RouterLogger.getCoreLogger().w("[Client] command \"%s\" callback success", command);
                    //result.setActivityStarted(resultCommand.isActivityStarted);
                    result.setExtra(resultCommand.extra);
                    result.setAddition(resultCommand.addition);
                    result.complete(request);
                    return null;
                }
            };
        } else {
            RouterLogger.getCoreLogger().d("[Client] request \"%s\" complete ahead of time", request.getNumber());
            result.complete(request);
        }
        execute(command);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass, String alias, Object feature, @Nullable Object... constructor) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] {serviceClass}, new RemoteHandler(serviceClass, alias, feature, constructor));
    }

    private class RemoteHandler implements InvocationHandler {

        private Class serviceClass;
        private String alias;
        private Object feature;
        private Object[] constructor;

        RemoteHandler(Class serviceClass, String alias, Object feature, @Nullable Object... constructor) {
            this.serviceClass = serviceClass;
            this.alias = alias;
            this.feature = feature;
            this.constructor = constructor;
        }

        @Override
        public Object invoke(Object proxy, Method method, @Nullable Object[] parameters) {
            final RemoteCommand command = new RemoteCommand(RemoteCommand.SERVICE);
            command.bridge = RemoteBridge.this;
            command.lifecycle = lifecycle;
            command.resend = resend;
            command.serviceClass = serviceClass;
            command.alias = alias;
            command.feature = feature;
            command.constructor = constructor;
            command.methodName = method.getName();
            command.parameters = parameters;
            RouterLogger.getCoreLogger().d("[Client] command: \"%s\" start IPC", command);
            RemoteResult result = execute(command);
            if (result != null && RemoteResult.SUCCESS.equals(result.state)) {
                return result.result;
            } else {
                Class<?> returnType = method.getReturnType();   // void、int...
                if (returnType.isPrimitive()) {
                    if (returnType == boolean.class) {
                        return false;
                    } else if (returnType == char.class) {
                        return '0';
                    } else {
                        return 0;
                    }
                } else {
                    return null;
                }
            }
        }
    }

    @Nullable
    // concurrent execute
    private RemoteResult execute(RemoteCommand command) {
        RouterLogger.getCoreLogger().d("[Client] execute command start, authority \"%s\", reTry:%s", authority, reTry);

        RemoteResult result = null;
        IHostService service = getHostService(authority);
        if (service != null) {
            try {
                retainResendCommand(command);
                result = service.execute(command);
                if (result != null) {
                    if (RemoteResult.SUCCESS.equals(result.state)) {
                        RouterLogger.getCoreLogger().d("[Client] command \"%s\" return and state success", command);
                    } else {
                        RouterLogger.getCoreLogger().e("[Client] command \"%s\" return and state fail", command);
                    }
                } else {
                    RouterLogger.getCoreLogger().e(
                            "[Client] command \"%s\" remote inner error with early termination", command);
                }
            } catch (RemoteException e) {     // dead object exception
                RouterLogger.getCoreLogger().e("[Client] remote exception: %s", e);
                if (!reTry) {
                    reTry = true;
                    sHostServiceMap.remove(authority);
                    return execute(command);
                }
            } catch (RuntimeException e) {    // remote exception will send here in some cases
                RouterLogger.getCoreLogger().e("[Client] remote exception: %s", e);
            }
        }
        RouterLogger.getCoreLogger().d("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        return result;
    }

    // After command execute
    private void retainResendCommand(final RemoteCommand command) {
        final String process = sProcessMap.get(authority);
        if (process == null) {
            RouterLogger.getCoreLogger().e("[Client] add resend command error, for process is null");
            return;
        }
        LifecycleOwner owner;
        final Lifecycle lifecycle = command.lifecycle != null ?
                ((owner = command.lifecycle.get()) != null ? owner.getLifecycle() : null) : null;
        if (command.resend) {
            if (lifecycle != null && lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
                RouterLogger.getCoreLogger().e("[Client] add resend command error, for lifecycle is destroyed");
                return;
            }
            Set<RemoteCommand> sResendCommands = sResendCommandMap.get(process);
            if (sResendCommands == null) {
                synchronized (ServiceLoader.class) {
                    sResendCommands = sResendCommandMap.get(process);
                    if (sResendCommands == null) {
                        sResendCommands = Collections.synchronizedSet(new ArraySet<RemoteCommand>());
                        sResendCommandMap.put(process, sResendCommands);
                    }
                }
            }
            if (!sResendCommands.contains(command)) {
                synchronized (this) {
                    if (!sResendCommands.contains(command)) {
                        sResendCommands.add(command);
                        if (lifecycle != null) {
                            lifecycle.addObserver(new GenericLifecycleObserver() {
                                @Override
                                public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
                                    if (event == Lifecycle.Event.ON_DESTROY) {
                                        Set<RemoteCommand> sResendCommands = sResendCommandMap.get(process);
                                        if (sResendCommands != null) {
                                            sResendCommands.remove(command);
                                            RouterLogger.getCoreLogger().w(
                                                    "[Client] remove resend command: \"%s\"", command);
                                        }
                                        lifecycle.removeObserver(this);
                                    }
                                }
                            });
                        }
                    }
                }
                RouterLogger.getCoreLogger().w(
                        "[Client] add resend command: \"%s\", with current lifecycle: %s",
                        command, lifecycle != null ? lifecycle.getCurrentState() : null);
            }
        }
    }

    private static void registerBroadcast(String process) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String process = intent.getStringExtra(FIELD_REMOTE_LAUNCH_ACTION);
                RouterLogger.getCoreLogger().w("receive broadcast remote app launcher process: \"%s\"", process);
                resendRemoteCommand(process);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_ACTION + process);
        DRouter.getContext().registerReceiver(receiver, filter);
    }

    private static void resendRemoteCommand(String process) {
        Set<RemoteCommand> commands = sResendCommandMap.get(process);
        if (commands != null) {
            for (RemoteCommand command : commands) {
                RouterLogger.getCoreLogger().w("execute resend command: \"%s\"", command);
                command.bridge.execute(command);
            }
        }
    }

    private static IHostService getHostService(final String authority) {
        IHostService service = sHostServiceMap.get(authority);
        if (service != null) {
            return service;
        }
        try {
            synchronized (RemoteCommand.class) {
                service = sHostServiceMap.get(authority);
                if (service != null) {
                    RouterLogger.getCoreLogger().d("[Client] getHostService get binder with cache");
                    return service;
                }
                Bundle bundle = null;
                for (int i = 0; i < 3; i++) {    // remote process killed case and retry, return null
                    try {
                        bundle = DRouter.getContext().getContentResolver().call(
                                Uri.parse(authority.startsWith("content://") ? authority : "content://" + authority),
                                "",
                                "",
                                null);
                    } catch (RuntimeException e) {
                        RouterLogger.getCoreLogger().e(
                                "[Client] getHostService call provider, try time %s, exception: %s", i, e.getMessage());
                    }
                    if (bundle != null) {
                        break;
                    }
                }
                boolean binderState = false, registerBroadcast = false;
                String process = "";
                if (bundle != null) {
                    bundle.setClassLoader(RemoteBridge.class.getClassLoader());
                    RemoteProvider.BinderParcel parcel = bundle.getParcelable(RemoteProvider.FIELD_REMOTE_BINDER);
                    process = bundle.getString(FIELD_REMOTE_PROCESS);

                    if (parcel != null) {
                        service = IHostService.Stub.asInterface(parcel.getBinder());
                        service.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                // no rebinding is required
                                sHostServiceMap.remove(authority);
                                RouterLogger.getCoreLogger().e(
                                        "[Client] linkToDeath: remote \"%s\" is died", authority);
                            }
                        }, 0);
                        sHostServiceMap.put(authority, service);
                        binderState = true;
                    }
                    if (process != null && !sProcessMap.containsKey(authority)) {
                        sProcessMap.put(authority, process);
                        registerBroadcast(process);
                        registerBroadcast = true;
                    }
                }
                RouterLogger.getCoreLogger().d("[Client] getHostService get binder: %s, process: \"%s\", " +
                        "register broadcast: %s", binderState, process, registerBroadcast);
                return service;
            }
        } catch (RemoteException e) {      // linkToDeath
            RouterLogger.getCoreLogger().e("[Client] getHostService remote exception: %s", e);
        }
        return null;
    }

    public static IBinder getHostBinder(String authority) {
        IHostService service = sHostServiceMap.get(authority);
        return service != null ? service.asBinder() : null;
    }

}
