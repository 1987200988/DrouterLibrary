/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.router;

import androidx.annotation.NonNull;

/**
 * Created by gaowei on 2019/2/27
 */
public interface IGlobalListener {

    void onComplete(@NonNull Request request, @NonNull Result result);
}
