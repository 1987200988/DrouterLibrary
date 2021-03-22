/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.store;

import com.didi.drouter.utils.RouterLogger;
import com.didi.drouter.utils.SystemUtil;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Created by gaowei on 2019-06-10
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LoadProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Log.d(RouterLogger.NAME, "[LoadProvider] onCreate and DRouter set application | " + getContext());
        SystemUtil.setApplication((Application) getContext());
        new Thread("drouter-init-thread") {
            @Override
            public void run() {
                Log.d(RouterLogger.NAME, "[LoadProvider] DRouter start load router table in drouter-init-thread");
                RouterStore.load(RouterStore.HOST);
            }
        }.start();
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
