/*
 * Copyright (C) 2021 Baidu, Inc. All Rights Reserved.
 */
package com.didi.drouter.utils;

import com.google.gson.Gson;

import androidx.annotation.RestrictTo;

/**
 * Created by gaowei on 2018/11/2
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class JsonConverter {

    private static JsonConvert jsonConvert;

    public static void setJsonConvert(JsonConvert convert) {
        jsonConvert = convert;
    }

    public static String toString(Object object) {
        check();
        return jsonConvert.toJson(object);
    }

    public static <T> T toObject(String json, Class<T> cls) {
        check();
        if (cls != null) {
            return jsonConvert.fromJson(json, cls);
        }
        return null;
    }

    private static void check() {
        if (jsonConvert == null) {
            jsonConvert = new InnerConvert();
        }
    }

    public interface JsonConvert {

        String toJson(Object src);

        <T> T fromJson(String json, Class<T> classOfT);
    }

    private static class InnerConvert implements JsonConvert {

        private final Gson gson = new Gson();

        @Override
        public String toJson(Object src) {
            return gson.toJson(src);
        }

        @Override
        public <T> T fromJson(String json, Class<T> cls) {
            return gson.fromJson(json, cls);
        }
    }
}
