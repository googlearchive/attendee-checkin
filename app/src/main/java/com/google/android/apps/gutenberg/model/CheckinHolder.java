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

package com.google.android.apps.gutenberg.model;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.google.android.apps.gutenberg.R;
import com.google.android.apps.gutenberg.util.RoundedImageListener;

public class CheckinHolder extends ViewHolder<Checkin> {

    private final ImageView mIcon;
    private final TextView mName;
    private final TextView mDescription;
    private final ImageView mCheckin;

    private boolean mWillAnimate;

    public CheckinHolder(LayoutInflater inflater, ViewGroup parent) {
        super(inflater.inflate(R.layout.item_timeline_checkin, parent, false));
        mIcon = (ImageView) itemView.findViewById(R.id.icon);
        mName = (TextView) itemView.findViewById(R.id.name);
        mDescription = (TextView) itemView.findViewById(R.id.description);
        mCheckin = (ImageView) itemView.findViewById(R.id.checkin);
    }

    @Override
    public void bind(Checkin checkin, ImageLoader imageLoader) {
        mName.setText(checkin.getAttendeeName());
        Context context = mDescription.getContext();
        long timestamp = checkin.getTimestamp();
        if (System.currentTimeMillis() - timestamp * 1000 < 60 * 1000) {
            mDescription.setText(R.string.just_now);
        } else {
            mDescription.setText(
                    DateUtils.getRelativeDateTimeString(context, timestamp * 1000,
                            DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0));
        }
        // Load the icon
        ImageLoader.ImageContainer container = (ImageLoader.ImageContainer) mIcon.getTag();
        if (container != null) {
            container.cancelRequest();
        }
        String imageUrl = checkin.getAttendeeImageUrl();
        if (!TextUtils.isEmpty(imageUrl) && Patterns.WEB_URL.matcher(imageUrl).matches()) {
            mIcon.setTag(imageLoader.get(imageUrl,
                    new RoundedImageListener(mIcon, R.drawable.ic_person,
                            R.drawable.ic_person)));
        } else {
            mIcon.setImageResource(R.drawable.ic_person);
        }
        if (mWillAnimate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mCheckin.setImageResource(R.drawable.checkin_anim);
        } else {
            mCheckin.setImageResource(R.drawable.checkin);
        }
    }

    public void setWillAnimate(boolean willAnimate) {
        mWillAnimate = willAnimate;
    }

    public void animateCheckin() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AnimatedVectorDrawable drawable = (AnimatedVectorDrawable) mCheckin.getDrawable();
            drawable.start();
        }
    }

}
