/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.store;

import com.didi.drouter.annotation.Thread;
import com.didi.drouter.interceptor.IInterceptor;
import com.didi.drouter.utils.TextUtils;

import android.net.Uri;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by gaowei on 2019/1/31
 */
public class RouterKey {

    Uri uri;
    Class<? extends IInterceptor>[] interceptor;
    int thread;
    boolean waiting;
    LifecycleOwner lifecycleOwner;

    private RouterKey(String uri) {
        this.uri = TextUtils.getUriKey(uri);
    }

    public static RouterKey build(String uri) {
        return new RouterKey(uri);
    }

    public void setThread(@Thread int thread) {
        this.thread = thread;
    }

    @SafeVarargs
    public final RouterKey setInterceptor(Class<? extends IInterceptor>... interceptor) {
        this.interceptor = interceptor;
        return this;
    }

    public RouterKey setWaiting(boolean waiting) {
        this.waiting = waiting;
        return this;
    }

    /**
     * @param owner Use FragmentActivity or support.v4.Fragment or LifecycleOwner,
     *              then you can use only DRouter.register() without DRouter.unregister() to match.
     * @return this
     */
    public RouterKey setLifecycleOwner(LifecycleOwner owner) {
        this.lifecycleOwner = owner;
        return this;
    }
}
