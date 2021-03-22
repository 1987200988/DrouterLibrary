/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.router;

import androidx.annotation.NonNull;

/**
 * Created by gaowei on 2018/9/5
 */
public interface RouterCallback {

    void onResult(@NonNull Result result);
}
