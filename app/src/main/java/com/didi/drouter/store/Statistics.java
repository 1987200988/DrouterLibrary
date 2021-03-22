/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.store;

import androidx.annotation.RestrictTo;

/**
 * Created by gaowei on 2018/11/29
 */
@SuppressWarnings("SpellCheckingInspection")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Statistics {

    static void init() {
//        RouterExecutor.main(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    OmegaSDK.trackEvent("drouter_app_name", new ArrayMap<String, Object>() {
//                        {
//                            put("name", SystemUtil.getAppName());
//                            put("package", SystemUtil.getPackageName());
//                        }
//                    });
//                } catch (Exception e) {
//                    // ignore
//                } catch (NoClassDefFoundError e) {
//                    // ignore
//                }
//            }
//        }, 20 * 1000);
    }

    public static void track(final String type) {
        try {
//            OmegaSDK.trackCounter("drouter_data_all");
//            OmegaSDK.trackCounter("drouter_data_" + type);
//            OmegaSDK.trackEvent("drouter_track_event", new ArrayMap<String, Object>() {
//                {
//                    put("type", type);
//                    if (!TextUtils.isEmpty(authority)) {
//                        put("authority", authority);
//                    }
//                }
//            });
        } catch (NoClassDefFoundError e) {
            // ignore
        }
    }
}
