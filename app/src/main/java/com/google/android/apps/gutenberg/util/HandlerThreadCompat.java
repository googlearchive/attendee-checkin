/*
 * Copyright 2015 Google Inc.
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

package com.google.android.apps.gutenberg.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.HandlerThread;

public class HandlerThreadCompat {

    private static final HandlerThreadCompatImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            IMPL = new JbMr2HandlerThreadCompatImpl();
        } else {
            IMPL = new BaseHandlerThreadCompatImpl();
        }
    }

    private HandlerThreadCompat() {
    }

    public static boolean quitSafely(HandlerThread thread) {
        return IMPL.quitSafely(thread);
    }

    private interface HandlerThreadCompatImpl {
        public boolean quitSafely(HandlerThread thread);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private static class JbMr2HandlerThreadCompatImpl implements HandlerThreadCompatImpl {

        @Override
        public boolean quitSafely(HandlerThread thread) {
            return thread.quitSafely();
        }
    }

    private static class BaseHandlerThreadCompatImpl implements HandlerThreadCompatImpl {

        @Override
        public boolean quitSafely(HandlerThread thread) {
            return thread.quit();
        }
    }

}
