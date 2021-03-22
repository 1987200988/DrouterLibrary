/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.router;

import androidx.annotation.NonNull;

/**
 * Created by gaowei on 2019/1/8
 */
public interface IRouterHandler {

    void handle(@NonNull Request request, @NonNull Result result);
}
