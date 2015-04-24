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

package com.google.android.apps.gutenberg.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class RecyclerViewSlidingUpPanelLayout extends SlidingUpPanelLayout {

    private RecyclerView mRecyclerView;

    private float mLastX;
    private float mLastY;

    public RecyclerViewSlidingUpPanelLayout(Context context) {
        this(context, null);
    }

    public RecyclerViewSlidingUpPanelLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerViewSlidingUpPanelLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mRecyclerView != null) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mLastX = ev.getX();
                    mLastY = ev.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(mLastX - ev.getX()) < Math.abs(mLastY - ev.getY())) {
                        if (mLastY < ev.getY()) { // Dragging down
                            return !mRecyclerView.canScrollVertically(-1);
                        } else { // Dragging up
                            return !mRecyclerView.canScrollVertically(1);
                        }
                    }
                    mLastX = ev.getX();
                    mLastY = ev.getY();
                    break;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

}
