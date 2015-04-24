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

import android.graphics.Bitmap;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.widget.ImageView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

public class RoundedImageListener implements ImageLoader.ImageListener {

    private final ImageView mImageView;
    private final int mDefaultImageResId;
    private final int mErrorImageResId;

    public RoundedImageListener(ImageView view, int defaultImageResId, int errorImageResId) {
        mImageView = view;
        mDefaultImageResId = defaultImageResId;
        mErrorImageResId = errorImageResId;
    }

    @Override
    public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
        if (response.getBitmap() != null) {
            Bitmap src = response.getBitmap();
            RoundedBitmapDrawable avatar = RoundedBitmapDrawableFactory.create(
                    mImageView.getResources(), src);
            avatar.setCornerRadius(Math.max(src.getWidth(), src.getHeight()) / 2.0f);
            mImageView.setImageDrawable(avatar);
        } else if (mDefaultImageResId != 0) {
            mImageView.setImageResource(mDefaultImageResId);
        }
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        if (mErrorImageResId != 0) {
            mImageView.setImageResource(mErrorImageResId);
        }
    }

}
