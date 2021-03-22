/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.visible;

import com.didi.drouter.router.Result;
import com.didi.drouter.router.RouterCallback;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by gaowei on 2018/9/12
 */
public abstract class ActivityCallback implements RouterCallback {

    /**
     * @param data activityResult
     */
    public abstract void onActivityResult(int resultCode, @Nullable Intent data);

    @Override
    public void onResult(@NonNull Result result) {

    }
}
