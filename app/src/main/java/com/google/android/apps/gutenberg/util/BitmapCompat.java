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
import android.graphics.Bitmap;
import android.os.Build;

/**
 * Compatibility utilities for {@link Bitmap}.
 */
public class BitmapCompat {

    /**
     * Retrieves the byte size of the bitmap.
     *
     * @param bitmap The bitmap
     * @return The size in bytes
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static int getAllocationByteCount(Bitmap bitmap) {
        if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
            return bitmap.getAllocationByteCount();
        } else if (Build.VERSION_CODES.HONEYCOMB_MR1 <= Build.VERSION.SDK_INT) {
            return bitmap.getByteCount();
        } else {
            return bitmap.getRowBytes() * bitmap.getHeight();
        }
    }

}
