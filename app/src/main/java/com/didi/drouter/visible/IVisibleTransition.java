/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.visible;



import com.didi.drouter.router.Request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by gaowei on 2019-05-27
 */
public interface IVisibleTransition {

    boolean transact(@NonNull Request request, @NonNull Class fragmentClass,
                     @Nullable Object fragmentInstance);
}
