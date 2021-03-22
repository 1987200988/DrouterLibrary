/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.api;

/**
 * Created by gaowei on 2018/9/13
 */
public interface Extend {

    /**
     * value:Bundle, 指定启动Activity的optionsBundle
     */
    String FIELD_START_ACTIVITY_OPTIONS = "field_start_activity_options";

    /**
     * value:int[], 指定启动Activity的动画
     */
    String FIELD_START_ACTIVITY_ANIMATION = "field_start_activity_animation";

    /**
     * value:int, 指定启动Activity的Flags
     */
    String FIELD_START_ACTIVITY_FLAGS = "field_start_activity_flags";

    /**
     * value:Intent, 使用intent启动Activity, 并忽略uri参数
     */
    String REQUEST_ACTIVITY_START_VIA_INTENT = "request_activity_start_via_intent";

    /**
     * value:int, 指定startActivityForResult的RequestCode
     */
    String FIELD_START_ACTIVITY_REQUEST_CODE = "field_start_activity_request_code";

    /**
     * value:int, 单位毫秒, 指定超时时间
     */
    String REQUEST_WAITING_TIMEOUT_PERIOD = "request_waiting_timeout_period";

    /**
     * 用于Activity
     * value:String, 格式 "scheme://host"
     * 在严格匹配的基础上，此设置会针对本次请求, 在只有path的activity注解前增加此scheme://host前缀, 得以匹配这个scheme和host
     */
    String REQUEST_ACTIVITY_DEFAULT_SCHEME_HOST = "request_activity_default_scheme_host";

    /**
     * 用于Fragment
     * value:Boolean，默认true
     * 是否由DRouter对fragment实例化
     */
    String REQUEST_FRAGMENT_NEW_INSTANCE = "request_fragment_new_instance";

    /**
     * 用于View
     * value:Boolean，默认true
     * 是否由DRouter对view实例化
     */
    String REQUEST_VIEW_NEW_INSTANCE = "request_view_new_instance";
}
