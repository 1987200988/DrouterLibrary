/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.visible;

import java.util.WeakHashMap;

import com.didi.drouter.api.Extend;
import com.didi.drouter.utils.RouterLogger;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by gaowei on 2018/9/12
 */
public class HoldFragmentForActivity extends Fragment {

    private static final String TAG = "HoldFragmentForActivity";

    private boolean attached;
    private static WeakHashMap<Activity, ActivityCallback> callback = new WeakHashMap<>();

    public HoldFragmentForActivity() {
        //RouterLogger.getCoreLogger().d("HoldFragment constructor");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //RouterLogger.getCoreLogger().d("HoldFragment onCreate");
        if (savedInstanceState != null) {
            attached = savedInstanceState.getBoolean("attached");
        }
    }

    public static void start(@NonNull Activity activity, @NonNull Intent intent, int requestCode, ActivityCallback callback) {
        HoldFragmentForActivity holdFragment = new HoldFragmentForActivity();
        HoldFragmentForActivity.callback.put(activity, callback);

        FragmentManager fragmentManager = activity.getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(holdFragment, TAG);
        transaction.commit();
        RouterLogger.getCoreLogger().d("ActivityResult HoldFragment commit attach");
        fragmentManager.executePendingTransactions();

        //RouterLogger.getCoreLogger().d("HoldFragment startActivityForResult");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holdFragment.startActivityForResult(intent, requestCode, intent.getBundleExtra(Extend.FIELD_START_ACTIVITY_OPTIONS));
        } else {
            holdFragment.startActivityForResult(intent, requestCode);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityCallback cb;
        if ((cb = callback.get(getActivity())) != null) {
            RouterLogger.getCoreLogger().d("ActivityResult callback");
            cb.onActivityResult(resultCode, data);
        } else {
            RouterLogger.getCoreLogger().d("ActivityResult callback fail for host activity destroyed");
        }
        callback.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        //RouterLogger.getCoreLogger().d("HoldFragment onResume");
        if (attached) {
            // second time
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.remove(this);
            transaction.commit();
            attached = false;
            RouterLogger.getCoreLogger().d("ActivityResult HoldFragment commit remove");
        }
        attached = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("attached", attached);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //RouterLogger.getCoreLogger().d("HoldFragment onDestroy");
    }
}
