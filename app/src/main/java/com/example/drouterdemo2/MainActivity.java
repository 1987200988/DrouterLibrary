/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.example.drouterdemo2;

import com.didi.drouter.api.DRouter;
import com.example.drouterdemo.ITest;

import android.os.Handler;
import android.os.RemoteCallbackList;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 调用远程Service
//        DRouter.build(IServiceTest.class)
//                .setRemoteAuthority("com.my.authority")
//                .getService(...)   //构造方法参数
//        .handle(...);      //目标方法参数
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                DRouter.build(ITest.class).setRemoteAuthority("com.my.authority").getService().test(0,null);
//
//            }
//        }, 3000);


    }
}
