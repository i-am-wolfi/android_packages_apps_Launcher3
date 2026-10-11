/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.util.override;

import android.content.Context;
import android.util.Log;

import androidx.annotation.UiThread;

import java.lang.reflect.Constructor;

import java.util.function.Function;

/**
 * Holder for an object that is initialized on the main thread, with support
 * for resource-based overrides (see {@link #forOverride}).
 */
public class MainThreadInitializedObject<T extends ResourceBasedOverride> {

    private static final String TAG = "MainThreadInitializedObject";

    private final Class<T> mClazz;
    private final int mResId;
    private final Function<Context, T> mProvider;
    private T mValue;

    public MainThreadInitializedObject(Class<T> clazz, int resId) {
        mClazz = clazz;
        mResId = resId;
    }

    public MainThreadInitializedObject(Function<Context, T> provider) {
        mClazz = null;
        mResId = 0;
        mProvider = provider;
    }

    public static <T extends ResourceBasedOverride> MainThreadInitializedObject<T> forOverride(
            Class<T> clazz, int resId) {
        return new MainThreadInitializedObject<>(clazz, resId);
    }

    /**
     * Returns the instance, creating the resource-specified override class
     * (with a {@link Context} constructor) when present, else a default.
     */
    @UiThread
    public synchronized T get(Context context) {
        if (mValue == null && mProvider != null) {
            mValue = mProvider.apply(context.getApplicationContext());
        }
        if (mValue == null) {
            String className = null;
            try {
                className = context.getString(mResId);
            } catch (Exception e) {
                Log.e(TAG, "Unable to load override resource", e);
            }
            if (className != null && !className.isEmpty()) {
                try {
                    Class<?> cls = Class.forName(className);
                    Constructor<?> ctor = cls.getDeclaredConstructor(Context.class);
                    ctor.setAccessible(true);
                    mValue = (T) ctor.newInstance(context.getApplicationContext());
                } catch (Exception e) {
                    Log.e(TAG, "Unable to create override class " + className, e);
                }
            }
            if (mValue == null) {
                try {
                    mValue = mClazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return mValue;
    }
}
